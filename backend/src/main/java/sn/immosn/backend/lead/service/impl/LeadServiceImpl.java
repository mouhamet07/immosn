package sn.immosn.backend.lead.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.lead.dto.*;
import sn.immosn.backend.client.web.lead.mapper.LeadMapper;
import sn.immosn.backend.lead.data.entity.Lead;
import sn.immosn.backend.lead.data.entity.StatutLead;
import sn.immosn.backend.lead.data.repository.LeadRepository;
import sn.immosn.backend.lead.service.LeadService;
import sn.immosn.backend.shared.exception.EntityNotFoundException;
import sn.immosn.backend.visite.data.repository.DemandeVisiteRepository;

@Service
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final AnnonceRepository annonceRepository;
    private final DemandeVisiteRepository visiteRepository;
    private final LeadMapper mapper;

    @Override
    @Transactional
    public LeadResponseDto create(LeadCreateRequestDto request) {
        var client  = userRepository.findById(request.clientId())
            .orElseThrow(() -> new EntityNotFoundException("Client non trouvé"));
        var annonce = annonceRepository.findByIdAndIsArchivedFalse(request.annonceId())
            .orElseThrow(() -> new EntityNotFoundException("Annonce non trouvée"));

        Lead.LeadBuilder builder = Lead.builder()
            .client(client)
            .annonce(annonce)
            .noteAdmin(request.noteAdmin());

        if (request.visiteId() != null) {
            var visite = visiteRepository.findByIdAndIsArchivedFalse(request.visiteId())
                .orElseThrow(() -> new EntityNotFoundException("Visite non trouvée"));
            builder.visite(visite);
        }

        return mapper.toDto(leadRepository.save(builder.build()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeadResponseDto> getAll(StatutLead statut, Pageable pageable) {
        Page<Lead> page = statut != null
            ? leadRepository.findByStatutOrderByCreatedAtDesc(statut, pageable)
            : leadRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public LeadResponseDto getById(Long id) {
        return mapper.toDto(leadRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Lead non trouvé avec l'ID: " + id)));
    }

    @Override
    @Transactional
    public LeadResponseDto updateStatut(Long id, UpdateStatutLeadDto dto) {
        Lead lead = leadRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Lead non trouvé avec l'ID: " + id));
        lead.setStatut(dto.statut());
        if (dto.noteAdmin() != null) lead.setNoteAdmin(dto.noteAdmin());
        return mapper.toDto(leadRepository.save(lead));
    }
}
