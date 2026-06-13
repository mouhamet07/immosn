package sn.immosn.backend.lead.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.client.web.lead.dto.LeadHistoryDto;
import sn.immosn.backend.lead.data.entity.Lead;
import sn.immosn.backend.lead.data.entity.LeadHistory;
import sn.immosn.backend.lead.data.entity.StatutLead;
import sn.immosn.backend.lead.data.repository.LeadHistoryRepository;
import sn.immosn.backend.lead.service.LeadHistoryService;
import sn.immosn.backend.shared.util.SecurityUtils;

@Service
@RequiredArgsConstructor
public class LeadHistoryServiceImpl implements LeadHistoryService {

    private final LeadHistoryRepository repository;

    @Override
    @Transactional
    public void record(Lead lead, StatutLead ancienStatut, StatutLead nouveauStatut,
                       String action, String commentaire) {
        LeadHistory h = LeadHistory.builder()
            .lead(lead)
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
    public Page<LeadHistoryDto> getHistory(Long leadId, Pageable pageable) {
        return repository.findByLeadIdOrderByCreatedAtDesc(leadId, pageable)
            .map(this::toDto);
    }

    private LeadHistoryDto toDto(LeadHistory h) {
        return new LeadHistoryDto(
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
