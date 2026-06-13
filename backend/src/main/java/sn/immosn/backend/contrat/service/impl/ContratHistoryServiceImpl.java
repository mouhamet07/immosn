package sn.immosn.backend.contrat.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.client.web.contrat.dto.ContratHistoryDto;
import sn.immosn.backend.contrat.data.entity.Contrat;
import sn.immosn.backend.contrat.data.entity.ContratHistory;
import sn.immosn.backend.contrat.data.entity.StatutContrat;
import sn.immosn.backend.contrat.data.repository.ContratHistoryRepository;
import sn.immosn.backend.contrat.service.ContratHistoryService;
import sn.immosn.backend.shared.util.SecurityUtils;

@Service
@RequiredArgsConstructor
public class ContratHistoryServiceImpl implements ContratHistoryService {

    private final ContratHistoryRepository repository;

    @Override
    @Transactional
    public void record(Contrat contrat, StatutContrat ancienStatut, StatutContrat nouveauStatut,
                       String action, String commentaire) {
        ContratHistory h = ContratHistory.builder()
            .contrat(contrat)
            .ancienStatut(ancienStatut)
            .nouveauStatut(nouveauStatut)
            .auteurId(SecurityUtils.getCurrentUserId())
            .auteurEmail(SecurityUtils.getCurrentUserEmail())
            .action(action)
            .commentaire(commentaire)
            .build();
        repository.save(h);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContratHistoryDto> getHistory(Long contratId, Pageable pageable) {
        return repository.findByContratIdOrderByCreatedAtDesc(contratId, pageable)
            .map(this::toDto);
    }

    private ContratHistoryDto toDto(ContratHistory h) {
        return new ContratHistoryDto(
            h.getId(),
            h.getAncienStatut()  != null ? h.getAncienStatut().name()  : null,
            h.getNouveauStatut() != null ? h.getNouveauStatut().name() : null,
            h.getAuteurId(),
            h.getAuteurEmail(),
            h.getAction(),
            h.getCommentaire(),
            h.getCreatedAt()
        );
    }
}
