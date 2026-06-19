package sn.immosn.backend.signalement.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.signalement.dto.*;
import sn.immosn.backend.client.web.signalement.mapper.SignalementMapper;
import sn.immosn.backend.contrat.data.entity.Contrat;
import sn.immosn.backend.contrat.data.repository.ContratRepository;
import sn.immosn.backend.shared.exception.EntityNotFoundException;
import sn.immosn.backend.signalement.data.entity.Signalement;
import sn.immosn.backend.signalement.data.entity.StatutSignalement;
import sn.immosn.backend.signalement.data.repository.SignalementRepository;
import sn.immosn.backend.signalement.service.SignalementEligibilite;
import sn.immosn.backend.signalement.service.SignalementHistoryService;
import sn.immosn.backend.signalement.service.SignalementService;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SignalementServiceImpl implements SignalementService {

    private static final Logger log = LoggerFactory.getLogger(SignalementServiceImpl.class);

    private final SignalementRepository signalementRepository;
    private final ContratRepository contratRepository;
    private final UserRepository userRepository;
    private final SignalementMapper mapper;
    private final SignalementEligibiliteAnalyzer eligibiliteAnalyzer;
    private final SignalementHistoryService historyService;

    /**
     * Crée un signalement et applique immédiatement les règles d'éligibilité contractuelle (Sprint 4).
     * VENTE : refus automatique (NON_ELIGIBLE) si déposé après la fin de garantie.
     * LOCATION : mise en analyse (EN_ANALYSE) avec catégorisation (entretien/réparation/dégradation/responsabilité).
     * La décision et son motif sont tracés dans l'historique.
     */
    @Override
    @Transactional
    public SignalementResponseDto create(SignalementCreateRequestDto request, String clientEmail) {
        var client = userRepository.findByEmail(clientEmail)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
        Contrat contrat = contratRepository.findByIdAndClientId(request.contratId(), client.getId())
            .orElseThrow(() -> new EntityNotFoundException("Contrat non trouvé"));

        LocalDate dateSignalement = LocalDate.now();
        SignalementEligibilite eligibilite =
            eligibiliteAnalyzer.analyser(contrat, request.contenu(), dateSignalement);

        Signalement sig = Signalement.builder()
            .contrat(contrat)
            .client(client)
            .contenu(request.contenu())
            .statut(eligibilite.statutInitial())
            .typeContratSnapshot(contrat.getTypeContrat())
            .build();

        if (!eligibilite.estEligible()) {
            sig.setMotifRejet(eligibilite.motif());
            sig.setDecisionAt(LocalDateTime.now());
        }

        Signalement saved = signalementRepository.save(sig);
        historyService.record(saved, null, eligibilite.statutInitial(),
            "CREATION", eligibilite.motif(), clientEmail);
        log.info("Signalement #{} créé (contrat #{}, type={}) → statut {} ({})",
            saved.getId(), contrat.getId(), contrat.getTypeContrat(),
            eligibilite.statutInitial(), eligibilite.categorie());

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
    @Transactional(readOnly = true)
    public SignalementResponseDto getById(Long id) {
        return mapper.toDto(load(id));
    }

    @Override
    @Transactional
    public SignalementResponseDto updateStatut(Long id, UpdateStatutSignalementDto dto) {
        Signalement sig = load(id);
        StatutSignalement ancien = sig.getStatut();
        StatutSignalement cible = dto.statut();

        // Sprint 4 : un rejet exige un motif (traçabilité de la décision).
        if (cible == StatutSignalement.REJETE
                && (dto.motifRejet() == null || dto.motifRejet().isBlank())) {
            throw new IllegalArgumentException("Le motif de rejet est obligatoire pour rejeter un signalement.");
        }

        sig.setStatut(cible);
        if (dto.reponseAdmin() != null) sig.setReponseAdmin(dto.reponseAdmin());
        if (dto.motifRejet() != null && !dto.motifRejet().isBlank()) {
            sig.setMotifRejet(dto.motifRejet());
        }
        // Horodatage de décision pour les statuts terminaux
        if (cible == StatutSignalement.ACCEPTE || cible == StatutSignalement.REJETE
                || cible == StatutSignalement.NON_ELIGIBLE) {
            sig.setDecisionAt(LocalDateTime.now());
        }

        Signalement saved = signalementRepository.save(sig);
        String motif = sig.getMotifRejet() != null ? sig.getMotifRejet() : dto.reponseAdmin();
        historyService.record(saved, ancien, cible,
            "CHANGEMENT_STATUT", motif, null);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public void markAsRead(Long id) {
        load(id);
        signalementRepository.markAsRead(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SignalementHistoryDto> getHistory(Long id, Pageable pageable) {
        load(id);
        return historyService.getHistory(id, pageable);
    }

    private Signalement load(Long id) {
        return signalementRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Signalement non trouvé avec l'ID: " + id));
    }
}
