package sn.immosn.backend.visite.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.client.web.visite.dto.VisiteHistoryDto;
import sn.immosn.backend.shared.util.SecurityUtils;
import sn.immosn.backend.visite.data.entity.DemandeVisite;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;
import sn.immosn.backend.visite.data.entity.VisiteHistory;
import sn.immosn.backend.visite.data.repository.VisiteHistoryRepository;
import sn.immosn.backend.visite.service.VisiteHistoryService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VisiteHistoryServiceImpl implements VisiteHistoryService {

    private final VisiteHistoryRepository repository;

    @Override
    @Transactional
    public void record(DemandeVisite visite, StatutDemandeVisite ancienStatut, StatutDemandeVisite nouveauStatut,
                       String action, String commentaire,
                       LocalDateTime ancienneDateVisite, LocalDateTime nouvelleDateVisite) {
        VisiteHistory h = VisiteHistory.builder()
            .visite(visite)
            .ancienStatut(ancienStatut)
            .nouveauStatut(nouveauStatut)
            .auteurId(SecurityUtils.getCurrentUserId())
            .auteurEmail(SecurityUtils.getCurrentUserEmail())
            .ancienneDateVisite(ancienneDateVisite)
            .nouvelleDateVisite(nouvelleDateVisite)
            .action(action)
            .commentaire(commentaire)
            .build();
        repository.save(h);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VisiteHistoryDto> getHistory(Long visiteId, Pageable pageable) {
        return repository.findByVisiteIdOrderByCreatedAtDesc(visiteId, pageable)
            .map(this::toDto);
    }

    private VisiteHistoryDto toDto(VisiteHistory h) {
        return new VisiteHistoryDto(
            h.getId(),
            h.getAncienStatut() != null ? h.getAncienStatut().name() : null,
            h.getNouveauStatut() != null ? h.getNouveauStatut().name() : null,
            h.getAuteurId(),
            h.getAuteurEmail(),
            h.getAncienneDateVisite(),
            h.getNouvelleDateVisite(),
            h.getAction(),
            h.getCommentaire(),
            h.getCreatedAt()
        );
    }
}
