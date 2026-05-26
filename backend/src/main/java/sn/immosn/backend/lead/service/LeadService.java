package sn.immosn.backend.lead.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sn.immosn.backend.client.web.lead.dto.*;
import sn.immosn.backend.lead.data.entity.StatutLead;

public interface LeadService {
    LeadResponseDto create(LeadCreateRequestDto request);
    Page<LeadResponseDto> getAll(StatutLead statut, Pageable pageable);
    LeadResponseDto getById(Long id);
    LeadResponseDto updateStatut(Long id, UpdateStatutLeadDto dto);
}
