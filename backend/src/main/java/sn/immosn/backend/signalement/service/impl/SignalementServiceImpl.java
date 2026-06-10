package sn.immosn.backend.signalement.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.signalement.dto.*;
import sn.immosn.backend.client.web.signalement.mapper.SignalementMapper;
import sn.immosn.backend.contrat.data.repository.ContratRepository;
import sn.immosn.backend.shared.exception.EntityNotFoundException;
import sn.immosn.backend.signalement.data.entity.Signalement;
import sn.immosn.backend.signalement.data.entity.StatutSignalement;
import sn.immosn.backend.signalement.data.repository.SignalementRepository;
import sn.immosn.backend.signalement.event.SignalementCreatedEvent;
import sn.immosn.backend.signalement.event.SignalementUpdatedEvent;
import sn.immosn.backend.signalement.service.SignalementService;

@Service
@RequiredArgsConstructor
public class SignalementServiceImpl implements SignalementService {

    private final SignalementRepository    signalementRepository;
    private final ContratRepository        contratRepository;
    private final UserRepository           userRepository;
    private final SignalementMapper        mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SignalementResponseDto create(SignalementCreateRequestDto request, String clientEmail) {
        var client  = userRepository.findByEmail(clientEmail)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
        var contrat = contratRepository.findByIdAndClientId(request.contratId(), client.getId())
            .orElseThrow(() -> new EntityNotFoundException("Contrat non trouvé"));

        Signalement sig = Signalement.builder()
            .contrat(contrat)
            .client(client)
            .contenu(request.contenu())
            .build();

        Signalement saved = signalementRepository.save(sig);
        eventPublisher.publishEvent(new SignalementCreatedEvent(
            saved.getId(), contrat.getId(),
            client.getEmail(), client.getId(), request.contenu()
        ));
        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SignalementResponseDto> getClientSignalements(String clientEmail, StatutSignalement statut, Pageable pageable) {
        var client = userRepository.findByEmail(clientEmail)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
        Page<Signalement> page = statut != null
            ? signalementRepository.findByClientIdAndStatutOrderByCreatedAtDesc(client.getId(), statut, pageable)
            : signalementRepository.findByClientIdOrderByCreatedAtDesc(client.getId(), pageable);
        return page.map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SignalementResponseDto> getAll(StatutSignalement statut, Pageable pageable) {
        Page<Signalement> page = statut != null
            ? signalementRepository.findByStatutOrderByCreatedAtDesc(statut, pageable)
            : signalementRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(mapper::toDto);
    }

    @Override
    @Transactional
    public SignalementResponseDto updateStatut(Long id, UpdateStatutSignalementDto dto) {
        Signalement sig = load(id);
        sig.setStatut(dto.statut());
        if (dto.reponseAdmin() != null) sig.setReponseAdmin(dto.reponseAdmin());
        Signalement saved = signalementRepository.save(sig);
        eventPublisher.publishEvent(new SignalementUpdatedEvent(
            saved.getId(), saved.getStatut().name(),
            saved.getClient().getEmail(), saved.getClient().getId(),
            dto.reponseAdmin() != null && !dto.reponseAdmin().isBlank()
        ));
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public void markAsRead(Long id) {
        load(id);
        signalementRepository.markAsRead(id);
    }

    private Signalement load(Long id) {
        return signalementRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Signalement non trouvé avec l'ID: " + id));
    }
}
