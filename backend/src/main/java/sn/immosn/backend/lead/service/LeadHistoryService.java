package sn.immosn.backend.lead.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sn.immosn.backend.client.web.lead.dto.LeadHistoryDto;
import sn.immosn.backend.lead.data.entity.Lead;
import sn.immosn.backend.lead.data.entity.StatutLead;

public interface LeadHistoryService {
    void record(Lead lead, StatutLead ancienStatut, StatutLead nouveauStatut,
                String action, String commentaire);
    Page<LeadHistoryDto> getHistory(Long leadId, Pageable pageable);
}
