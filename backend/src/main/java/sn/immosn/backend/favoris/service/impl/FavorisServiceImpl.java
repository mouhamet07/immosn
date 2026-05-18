package sn.immosn.backend.favoris.service.impl;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class FavorisServiceImpl implements FavorisService {

    private final AnnonceFavorisRepository favorisRepository;
    private final AnnonceRepository annonceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public FavorisStatusDto toggle(Long annonceId, String clientEmail) {
        var client  = userRepository.findByEmail(clientEmail)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
        var annonce = annonceRepository.findByIdAndIsArchivedFalse(annonceId)
            .orElseThrow(() -> new EntityNotFoundException("Annonce non trouvée"));

        boolean exists = favorisRepository.existsByClientIdAndAnnonceId(client.getId(), annonceId);
        if (exists) {
            favorisRepository.deleteByClientIdAndAnnonceId(client.getId(), annonceId);
            return new FavorisStatusDto(annonceId, false);
        } else {
            AnnonceFavoris fav = AnnonceFavoris.builder()
                .client(client)
                .annonce(annonce)
                .build();
            favorisRepository.save(fav);
            return new FavorisStatusDto(annonceId, true);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FavorisResponseDto> getClientFavoris(String clientEmail, Pageable pageable) {
        var client = userRepository.findByEmail(clientEmail)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

        return favorisRepository
            .findByClientIdOrderByCreatedAtDesc(client.getId(), pageable)
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
        boolean isFav = favorisRepository.existsByClientIdAndAnnonceId(client.getId(), annonceId);
        return new FavorisStatusDto(annonceId, isFav);
    }
}
