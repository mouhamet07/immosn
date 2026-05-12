package sn.immosn.backend.annonce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.annonce.data.entity.AnnonceCommodite;
import sn.immosn.backend.annonce.data.entity.Commodite;
import sn.immosn.backend.annonce.data.entity.TypeBienAnnonce;
import sn.immosn.backend.annonce.data.repository.AnnonceCommoditesRepository;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.annonce.data.repository.CommoditeRepository;
import sn.immosn.backend.annonce.data.repository.TypeBienAnnonceRepository;
import sn.immosn.backend.annonce.service.AnnonceService;
import sn.immosn.backend.client.web.annonce.dto.AnnonceCreateRequestDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceListDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceResponseDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceUpdateRequestDto;
import sn.immosn.backend.client.web.annonce.mapper.AnnonceMapper;

import java.util.List;

@Service
@RequiredArgsConstructor

public class AnnonceServiceImpl implements AnnonceService {

    private final AnnonceRepository annonceRepository;
    private final CommoditeRepository commoditeRepository;
    private final TypeBienAnnonceRepository typeBienAnnonceRepository;
    private final AnnonceMapper annonceMapper;



    @Override
    public AnnonceResponseDto createAnnonce(AnnonceCreateRequestDto requestDto) {

        TypeBienAnnonce typeBien = typeBienAnnonceRepository
                .findById(requestDto.typeBienId())
                .orElseThrow(() -> new RuntimeException("Type de bien non trouvé"));

        if (typeBien.getIsArchived()) {
            throw new RuntimeException("Ce type de bien est archivé");
        }

        List<Commodite> commodites = requestDto.commoditeIds() != null
                ? commoditeRepository.findByIdInAndIsArchivedFalse(requestDto.commoditeIds())
                : List.of();

        Annonce annonce = annonceMapper.toEntity(requestDto, typeBien, commodites);

        for (Commodite commodite : commodites) {
            AnnonceCommodite ac = AnnonceCommodite.builder()
                    .annonce(annonce)
                    .commodite(commodite)
                    .build();
            annonce.getAnnonceCommodites().add(ac);
        }

        Annonce savedAnnonce = annonceRepository.save(annonce);
        return annonceMapper.toResponse(savedAnnonce);
    }

    @Override
    public AnnonceResponseDto getAnnonceById(Long id) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Annonce non trouvée avec l'ID: " + id));
        if (annonce.getArchived()) {
            throw new RuntimeException("Annonce archivée avec l'ID: " + id);
        }
        return annonceMapper.toResponse(annonce);
    }

    @Override
    public Page<AnnonceListDto> getAllAnnonces(Pageable pageable) {
        return annonceRepository
                .findByArchivedFalse(pageable)
                .map(annonceMapper::toListDto);
    }

    @Override
    public AnnonceResponseDto updateAnnonce(Long id, AnnonceUpdateRequestDto requestDto) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Annonce non trouvée"));

        if (annonce.getArchived()) {
            throw new RuntimeException("Impossible de modifier une annonce archivée");
        }

        if (requestDto.libelle() != null) annonce.setLibelle(requestDto.libelle());
        if (requestDto.description() != null) annonce.setDescription(requestDto.description());
        if (requestDto.nbrPieces() != null) annonce.setNbrPieces(requestDto.nbrPieces());
        if (requestDto.surface() != null) annonce.setSurface(requestDto.surface());
        if (requestDto.prix() != null) annonce.setPrix(requestDto.prix());
        if (requestDto.adresse() != null) annonce.setAdresse(requestDto.adresse());
        if (requestDto.images() != null) annonce.setImages(requestDto.images());


        if (requestDto.typeBienId() != null) {
            TypeBienAnnonce typeBien = typeBienAnnonceRepository
                    .findById(requestDto.typeBienId())
                    .orElseThrow(() -> new RuntimeException("Type de bien non trouvé"));
            if (typeBien.getIsArchived()) {
                throw new RuntimeException("Ce type de bien est archivé");
            }
            annonce.setTypeBien(typeBien);
        }


        if (requestDto.commoditeIds() != null) {
            List<Commodite> commodites = commoditeRepository.findByIdInAndIsArchivedFalse(requestDto.commoditeIds());
            annonce.getAnnonceCommodites().clear();
            for (Commodite commodite : commodites) {
                AnnonceCommodite ac = AnnonceCommodite.builder()
                        .annonce(annonce)
                        .commodite(commodite)
                        .build();
                annonce.getAnnonceCommodites().add(ac);
            }
        }

        Annonce updatedAnnonce = annonceRepository.save(annonce);
        return annonceMapper.toResponse(updatedAnnonce);
    }

    @Override
    public void archiveAnnonce(Long id) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Annonce non trouvée avec l'ID: " + id));

        if (annonce.getArchived()) {
            throw new RuntimeException("Cette annonce est déjà archivée");
        }

        annonce.setArchived(true);
        annonceRepository.save(annonce);
    }

    @Override
    public void restoreAnnonce(Long id) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Annonce non trouvée avec l'ID: " + id));

        if (!annonce.getArchived()) {
            throw new RuntimeException("Cette annonce n'est pas archivée");
        }

        annonce.setArchived(false);
        annonceRepository.save(annonce);
    }

    @Override
    public Page<AnnonceListDto> getAllAnnoncesAdmin(Pageable pageable) {
        return annonceRepository
                .findAll(pageable)
                .map(annonceMapper::toListDto);
    }
}
