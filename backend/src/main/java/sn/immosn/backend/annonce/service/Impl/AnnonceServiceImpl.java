package sn.immosn.backend.annonce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.annonce.data.entity.AnnonceCommodite;
import sn.immosn.backend.annonce.data.entity.Commodite;
import sn.immosn.backend.annonce.data.entity.TypeBienAnnonce;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.annonce.data.repository.AnnonceSpecification;
import sn.immosn.backend.annonce.data.repository.CommoditeRepository;
import sn.immosn.backend.annonce.data.repository.TypeBienAnnonceRepository;
import sn.immosn.backend.annonce.service.AnnonceService;
import sn.immosn.backend.client.web.annonce.dto.AnnonceCreateRequestDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceListDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceResponseDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceUpdateRequestDto;
import sn.immosn.backend.client.web.annonce.dto.SearchAnnonceRequestDto;
import sn.immosn.backend.client.web.annonce.mapper.AnnonceMapper;
import sn.immosn.backend.contrat.data.entity.StatutContrat;
import sn.immosn.backend.contrat.data.repository.ContratRepository;
import sn.immosn.backend.location.service.GeoCodingService;
import sn.immosn.backend.shared.exception.EntityNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnonceServiceImpl implements AnnonceService {

    private static final Logger log = LoggerFactory.getLogger(AnnonceServiceImpl.class);

    private final AnnonceRepository         annonceRepository;
    private final CommoditeRepository       commoditeRepository;
    private final TypeBienAnnonceRepository typeBienAnnonceRepository;
    private final AnnonceMapper             annonceMapper;
    private final ContratRepository         contratRepository;
    private final GeoCodingService          geoCodingService;

    @Override
    @Transactional
    public AnnonceResponseDto createAnnonce(AnnonceCreateRequestDto requestDto) {
        log.info("Création d'une annonce : libelle={}", requestDto.libelle());

        TypeBienAnnonce typeBien = typeBienAnnonceRepository
            .findByIdAndIsArchivedFalse(requestDto.typeBienId())
            .orElseThrow(() -> new EntityNotFoundException("Type de bien non trouvé : id=" + requestDto.typeBienId()));

        List<Commodite> commodites = requestDto.commoditeIds() != null
            ? commoditeRepository.findByIdInAndIsArchivedFalse(requestDto.commoditeIds())
            : List.of();

        Annonce annonce = annonceMapper.toEntity(requestDto, typeBien, commodites);
        annonce.setRegion(resolveRegion(requestDto.region()));
        annonce.setAdresse(buildFinalAdresse(requestDto.adresseExacte(), annonce.getQuartier(), annonce.getDepartement()));

        String rawGeocodeAddress = normalize(requestDto.adresseExacte());
        boolean geoOk = applyGeolocation(annonce, annonce.getQuartier(), annonce.getDepartement(), rawGeocodeAddress);
        Annonce saved = annonceRepository.save(annonce);

        if (geoOk) geoCodingService.logGeolocation(saved.getId());
        else        geoCodingService.logGeolocationFailed(saved.getId());
        log.info("Annonce créée : id={}", saved.getId());
        return annonceMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AnnonceResponseDto getAnnonceById(Long id) {
        Annonce annonce = annonceRepository.findAnnonceByIdWithImages(id)
            .orElseThrow(() -> new EntityNotFoundException("Annonce non trouvée : id=" + id));
        return annonceMapper.toResponse(annonce);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnnonceListDto> getAllAnnonces(Pageable pageable) {
        return annonceRepository.findByIsArchivedFalse(pageable).map(annonceMapper::toListDto);
    }

    @Override
    @Transactional
    public AnnonceResponseDto updateAnnonce(Long id, AnnonceUpdateRequestDto requestDto) {
        log.info("Modification annonce : id={}", id);

        Annonce annonce = annonceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Annonce non trouvée : id=" + id));

        if (requestDto.libelle()     != null) annonce.setLibelle(requestDto.libelle());
        if (requestDto.description() != null) annonce.setDescription(requestDto.description());
        if (requestDto.nbrPieces()   != null) annonce.setNbrPieces(requestDto.nbrPieces());
        if (requestDto.surface()     != null) annonce.setSurface(requestDto.surface());
        if (requestDto.prix()        != null) annonce.setPrix(requestDto.prix());
        if (requestDto.images()      != null) annonce.setImages(requestDto.images());

        boolean locationChanged = requestDto.adresseExacte() != null
            || requestDto.departement() != null
            || requestDto.quartier()    != null
            || requestDto.region()      != null;

        if (requestDto.departement() != null) {
            if (requestDto.departement().isBlank()) {
                throw new IllegalArgumentException("Le département ne peut pas être vide");
            }
            annonce.setDepartement(requestDto.departement().trim());
        }
        if (requestDto.quartier() != null) {
            if (requestDto.quartier().isBlank()) {
                throw new IllegalArgumentException("Le quartier ne peut pas être vide");
            }
            annonce.setQuartier(requestDto.quartier().trim());
        }
        if (requestDto.region() != null) {
            if (requestDto.region().isBlank()) {
                throw new IllegalArgumentException("La région ne peut pas être vide");
            }
            annonce.setRegion(requestDto.region().trim());
        }

        if (locationChanged) {
            annonce.setAdresse(buildFinalAdresse(requestDto.adresseExacte(), annonce.getQuartier(), annonce.getDepartement()));
            if (annonce.getRegion() == null) {
                annonce.setRegion("Dakar");
            }
        }

        boolean geoOk = false;
        if (locationChanged) {
            geoOk = applyGeolocation(annonce, annonce.getQuartier(), annonce.getDepartement(), normalize(requestDto.adresseExacte()));
        }

        if (requestDto.typeBienId() != null) {
            TypeBienAnnonce typeBien = typeBienAnnonceRepository
                .findByIdAndIsArchivedFalse(requestDto.typeBienId())
                .orElseThrow(() -> new EntityNotFoundException("Type de bien non trouvé"));
            annonce.setTypeBien(typeBien);
        }

        if (requestDto.commoditeIds() != null) {
            List<Commodite> commodites = commoditeRepository.findByIdInAndIsArchivedFalse(requestDto.commoditeIds());
            annonce.getAnnonceCommodites().clear();
            commodites.forEach(c -> annonce.getAnnonceCommodites().add(
                AnnonceCommodite.builder().annonce(annonce).commodite(c).build()
            ));
        }

        if (annonce.getRegion() == null) {
            annonce.setRegion("Dakar");
        }

        Annonce updated = annonceRepository.save(annonce);
        if (locationChanged) {
            if (geoOk) geoCodingService.logGeolocation(updated.getId());
            else        geoCodingService.logGeolocationFailed(updated.getId());
        }
        log.info("Annonce modifiée : id={}", updated.getId());
        return annonceMapper.toResponse(updated);
    }

    /**
     * Archivage (soft-delete).
     * RÈGLE MÉTIER : impossible d'archiver si un contrat ACTIF est lié à cette annonce.
     */
    @Override
    @Transactional
    public void archiveAnnonce(Long id) {
        Annonce annonce = annonceRepository.findByIdAndIsArchivedFalse(id)
            .orElseThrow(() -> new EntityNotFoundException("Annonce non trouvée : id=" + id));

        long contratsActifs = contratRepository.countByAnnonceIdAndStatut(id, StatutContrat.ACTIF);
        if (contratsActifs > 0) {
            log.warn("Archivage refusé pour annonce id={} : {} contrat(s) actif(s)", id, contratsActifs);
            throw new IllegalStateException(
                "Impossible d'archiver cette annonce : " + contratsActifs +
                " contrat(s) actif(s) y est(sont) lié(s). Résiliez les contrats avant d'archiver."
            );
        }

        annonce.setArchived(true);
        annonceRepository.save(annonce);
        log.info("Annonce archivée : id={}", id);
    }

    @Override
    @Transactional
    public void restoreAnnonce(Long id) {
        Annonce annonce = annonceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Annonce non trouvée : id=" + id));
        annonce.setArchived(false);
        annonceRepository.save(annonce);
        log.info("Annonce restaurée : id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnnonceListDto> getAllAnnoncesAdmin(Pageable pageable) {
        return annonceRepository.findAll(pageable).map(annonceMapper::toListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public AnnonceResponseDto getAnnonceByIdAdmin(Long id) {
        Annonce annonce = annonceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Annonce non trouvée : id=" + id));
        return annonceMapper.toResponse(annonce);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnnonceListDto> searchAnnonces(SearchAnnonceRequestDto request) {
        int    page  = request.page()   != null ? request.page()   : 0;
        int    size  = request.size()   != null ? request.size()   : 9;
        String sort  = (request.sortBy() != null && !request.sortBy().isBlank()) ? request.sortBy() : "createdAt";
        Sort.Direction dir = "ASC".equalsIgnoreCase(request.sortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort));
        Specification<Annonce> spec = AnnonceSpecification.fromRequest(request);
        return annonceRepository.findAll(spec, pageable).map(annonceMapper::toListDto);
    }

    private boolean applyGeolocation(Annonce annonce, String quartier, String departement, String adresse) {
        if (quartier == null || departement == null) return false;
        return geoCodingService.geocode(quartier, departement, adresse)
            .map(coords -> {
                annonce.setLatitude(coords[0]);
                annonce.setLongitude(coords[1]);
                return true;
            })
            .orElseGet(() -> {
                log.warn("Coordonnées introuvables pour quartier='{}', departement='{}'", quartier, departement);
                return false;
            });
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveRegion(String region) {
        String normalized = normalize(region);
        return normalized != null ? normalized : "Dakar";
    }

    private String buildFinalAdresse(String adresseExacte, String quartier, String departement) {
        String normalizedQuartier = normalize(quartier);
        String normalizedDepartement = normalize(departement);
        if (normalizedQuartier == null || normalizedDepartement == null) {
            throw new IllegalArgumentException("Le quartier et le département sont obligatoires pour construire l'adresse finale");
        }
        String exact = normalize(adresseExacte);
        return exact != null
            ? exact + ", " + normalizedQuartier + ", " + normalizedDepartement
            : normalizedQuartier + ", " + normalizedDepartement;
    }
}
