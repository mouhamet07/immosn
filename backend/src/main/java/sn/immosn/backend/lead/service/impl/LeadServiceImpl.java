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
import sn.immosn.backend.lead.service.LeadHistoryService;
import sn.immosn.backend.lead.service.LeadService;
import sn.immosn.backend.shared.exception.EntityNotFoundException;
import sn.immosn.backend.visite.data.repository.DemandeVisiteRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    /**
     * Machine à états du pipeline Lead.
     * EN_COURS   → CONVERTI | ABANDONNE
     * CONVERTI   → (aucune) — état final
     * ABANDONNE  → (aucune) — état final
     */
    private static final Map<StatutLead, Set<StatutLead>> TRANSITIONS_AUTORISEES = Map.of(
        StatutLead.EN_COURS,  Set.of(StatutLead.CONVERTI, StatutLead.ABANDONNE),
        StatutLead.CONVERTI,  Set.of(),
        StatutLead.ABANDONNE, Set.of()
    );

    private final LeadRepository          leadRepository;
    private final UserRepository          userRepository;
    private final AnnonceRepository       annonceRepository;
    private final DemandeVisiteRepository visiteRepository;
    private final LeadMapper              mapper;
    private final LeadHistoryService      leadHistoryService;

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
            if (leadRepository.existsByVisiteId(request.visiteId())) {
                throw new IllegalStateException(
                    "Un lead existe déjà pour la visite #" + request.visiteId()
                    + ". Une visite ne peut être liée qu'à un seul lead.");
            }
            var visite = visiteRepository.findByIdAndIsArchivedFalse(request.visiteId())
                .orElseThrow(() -> new EntityNotFoundException("Visite non trouvée"));
            builder.visite(visite);
        }

        Lead saved = leadRepository.save(builder.build());
        leadHistoryService.record(saved, null, StatutLead.EN_COURS, "CREATION",
            request.noteAdmin() != null ? "Note initiale : " + request.noteAdmin() : null);
        return mapper.toDto(saved);
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
        StatutLead ancienStatut = lead.getStatut();
        validateTransition(ancienStatut, dto.statut());
        lead.setStatut(dto.statut());
        if (dto.statut() == StatutLead.CONVERTI && lead.getConvertedAt() == null) {
            lead.setConvertedAt(LocalDateTime.now());
        }
        if (dto.noteAdmin() != null) lead.setNoteAdmin(dto.noteAdmin());
        Lead saved = leadRepository.save(lead);
        String action = dto.statut() == StatutLead.CONVERTI ? "CONVERSION" : "ABANDON";
        leadHistoryService.record(saved, ancienStatut, dto.statut(), action, dto.noteAdmin());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public LeadResponseDto updateNote(Long id, UpdateNoteLeadDto dto) {
        Lead lead = leadRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Lead non trouvé avec l'ID: " + id));
        lead.setNoteAdmin(dto.note());
        Lead saved = leadRepository.save(lead);
        leadHistoryService.record(saved, null, null, "NOTE_MODIFIEE",
            dto.note() != null ? dto.note() : "(note effacée)");
        return mapper.toDto(saved);
    }

    private void validateTransition(StatutLead actuel, StatutLead cible) {
        Set<StatutLead> permis = TRANSITIONS_AUTORISEES.getOrDefault(actuel, Set.of());
        if (!permis.contains(cible)) {
            String autorisees = permis.isEmpty() ? "aucune — état final" : permis.toString();
            throw new IllegalStateException(
                "Transition de statut invalide : " + actuel + " → " + cible
                + ". Transitions autorisées depuis " + actuel + " : " + autorisees + ".");
        }
    }
}
