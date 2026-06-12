package sn.immosn.backend.favoris.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.favoris.dto.FavorisResponseDto;
import sn.immosn.backend.client.web.favoris.dto.FavorisStatusDto;
import sn.immosn.backend.favoris.data.entity.AnnonceFavoris;
import sn.immosn.backend.favoris.data.repository.AnnonceFavorisRepository;
import sn.immosn.backend.favoris.service.FavorisService;
import sn.immosn.backend.shared.exception.EntityNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavorisServiceImpl implements FavorisService {

    private static final Logger log = LoggerFactory.getLogger(FavorisServiceImpl.class);

    private final AnnonceFavorisRepository favorisRepository;
    private final AnnonceRepository        annonceRepository;
    private final UserRepository           userRepository;

    @Override
    @Transactional
    public FavorisStatusDto toggle(Long annonceId, String clientEmail) {
        var client  = userRepository.findByEmail(clientEmail)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
        var annonce = annonceRepository.findByIdAndIsArchivedFalse(annonceId)
            .orElseThrow(() -> new EntityNotFoundException("Annonce non trouvée : id=" + annonceId));

        boolean exists = favorisRepository.existsByClientIdAndAnnonceId(client.getId(), annonceId);
        if (exists) {
            favorisRepository.deleteByClientIdAndAnnonceId(client.getId(), annonceId);
            log.debug("Favori retiré : clientId={}, annonceId={}", client.getId(), annonceId);
            return new FavorisStatusDto(annonceId, false);
        } else {
            AnnonceFavoris fav = AnnonceFavoris.builder()
                .client(client)
                .annonce(annonce)
                .build();
            favorisRepository.save(fav);
            log.debug("Favori ajouté : clientId={}, annonceId={}", client.getId(), annonceId);
            return new FavorisStatusDto(annonceId, true);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FavorisResponseDto> getClientFavoris(String clientEmail, Pageable pageable) {
        var client = userRepository.findByEmail(clientEmail)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

        return favorisRepository
            .findActiveByClientId(client.getId(), pageable)
            .map(fav -> {
                var a = fav.getAnnonce();
                String image = (a.getImages() != null && !a.getImages().isEmpty())
                    ? a.getImages().get(0) : null;
                return new FavorisResponseDto(
                    a.getId(),
                    a.getLibelle(),
                    a.getPrix(),
                    a.getAdresse(),
                    a.getTypeBien() != null ? a.getTypeBien().getLibelle() : null,
                    a.getNbrPieces(),
                    a.getSurface(),
                    image,
                    fav.getCreatedAt()
                );
            });
    }

    @Override
    @Transactional(readOnly = true)
    public FavorisStatusDto checkFavoris(Long annonceId, String clientEmail) {
        var client = userRepository.findByEmail(clientEmail)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
        // Utilise existsActiveByClientIdAndAnnonceId pour exclure les annonces archivées
        // cohérence avec getAllFavorisIds() et getClientFavoris() qui filtrent déjà isArchived=false
        boolean isFav = favorisRepository.existsActiveByClientIdAndAnnonceId(client.getId(), annonceId);
        return new FavorisStatusDto(annonceId, isFav);
    }

    /**
     * Retourne tous les IDs d'annonces favorites sans limite fixe.
     * Remplace l'ancienne approche qui chargeait les 100 premiers IDs en dur.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Long> getAllFavorisIds(String clientEmail) {
        var client = userRepository.findByEmail(clientEmail)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

        return favorisRepository
            .findAllActiveByClientId(client.getId())
            .stream()
            .map(fav -> fav.getAnnonce().getId())
            .toList();
    }
}
