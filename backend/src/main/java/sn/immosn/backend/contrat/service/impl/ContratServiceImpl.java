package sn.immosn.backend.contrat.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.contrat.dto.*;
import sn.immosn.backend.client.web.contrat.mapper.ContratMapper;
import sn.immosn.backend.contrat.data.entity.Contrat;
import sn.immosn.backend.contrat.data.entity.StatutContrat;
import sn.immosn.backend.contrat.data.entity.TypeContrat;
import sn.immosn.backend.contrat.data.repository.ContratRepository;
import sn.immosn.backend.contrat.service.ContratService;
import sn.immosn.backend.lead.data.entity.Lead;
import sn.immosn.backend.lead.data.entity.StatutLead;
import sn.immosn.backend.lead.data.repository.LeadRepository;
import sn.immosn.backend.shared.exception.EntityNotFoundException;
import sn.immosn.backend.visite.data.entity.DemandeVisite;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContratServiceImpl implements ContratService {

    private static final Logger log = LoggerFactory.getLogger(ContratServiceImpl.class);

    private final ContratRepository contratRepository;
    private final UserRepository    userRepository;
    private final AnnonceRepository annonceRepository;
    private final LeadRepository    leadRepository;
    private final ContratMapper     mapper;

    @Override
    @Transactional
    public ContratResponseDto create(ContratCreateRequestDto request) {
        log.info("Création contrat : clientId={}, annonceId={}", request.clientId(), request.annonceId());

        var client  = userRepository.findById(request.clientId())
            .orElseThrow(() -> new EntityNotFoundException("Client non trouvé : id=" + request.clientId()));
        var annonce = annonceRepository.findByIdAndIsArchivedFalse(request.annonceId())
            .orElseThrow(() -> new EntityNotFoundException("Annonce non trouvée : id=" + request.annonceId()));

        Contrat.ContratBuilder builder = Contrat.builder()
            .client(client)
            .annonce(annonce)
            .dateDebut(request.dateDebut())
            .dateFin(request.dateFin())
            .montant(request.montant())
            .documentUrl(request.documentUrl())
            .notes(request.notes());

        if (request.leadId() != null) {
            var lead = leadRepository.findById(request.leadId())
                .orElseThrow(() -> new EntityNotFoundException("Lead non trouvé : id=" + request.leadId()));
            builder.lead(lead);
            lead.setStatut(StatutLead.CONVERTI);
            leadRepository.save(lead);
        }

        Contrat saved = contratRepository.save(builder.build());
        log.info("Contrat créé : id={}", saved.getId());
        return mapper.toDto(saved);
    }

    /**
     * Création automatique depuis une visite clôturée.
     * VENTE    : montant = annonce.prix (prix de cession).
     * LOCATION : montant = annonce.prix × dureeLocationMois (loyer mensuel × durée).
     *            dateFin = dateDebut + dureeLocationMois mois.
     */
    @Override
    @Transactional
    public ContratResponseDto createFromVisite(DemandeVisite visite, TypeContrat typeContrat, Integer dureeLocationMois) {
        if (typeContrat == null) {
            throw new IllegalArgumentException("Le type de contrat est obligatoire");
        }
        if (typeContrat == TypeContrat.LOCATION && (dureeLocationMois == null || dureeLocationMois <= 0)) {
            throw new IllegalArgumentException("La durée en mois est obligatoire pour un contrat de LOCATION");
        }

        List<Lead> leads = leadRepository.findByVisiteId(visite.getId());
        Lead lead = leads.isEmpty() ? null : leads.get(0);
        if (lead == null) {
            log.warn("Aucun lead trouvé pour visite #{} — contrat créé sans lead. Le statut CONVERTI ne sera pas positionné.", visite.getId());
        }

        LocalDate dateDebut = LocalDate.now();
        BigDecimal prixAnnonce = visite.getAnnonce().getPrix();

        BigDecimal montant = (typeContrat == TypeContrat.LOCATION)
            ? montantLocation(prixAnnonce, dureeLocationMois)
            : prixAnnonce;

        LocalDate dateFin = (typeContrat == TypeContrat.LOCATION)
            ? dateFinLocation(dateDebut, dureeLocationMois)
            : null;

        Contrat.ContratBuilder builder = Contrat.builder()
            .client(visite.getClient())
            .annonce(visite.getAnnonce())
            .dateDebut(dateDebut)
            .dateFin(dateFin)
            .montant(montant)
            .typeContrat(typeContrat)
            .dureeLocationMois(typeContrat == TypeContrat.LOCATION ? dureeLocationMois : null);

        if (lead != null) {
            builder.lead(lead);
            lead.setStatut(StatutLead.CONVERTI);
            leadRepository.save(lead);
        }

        Contrat saved = contratRepository.save(builder.build());
        log.info("Contrat auto-créé depuis visite #{} : contratId={}, type={}", visite.getId(), saved.getId(), typeContrat);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContratResponseDto> getClientContrats(String clientEmail, StatutContrat statut, Pageable pageable) {
        var client = userRepository.findByEmail(clientEmail)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé : email=" + clientEmail));
        Page<Contrat> page = statut != null
            ? contratRepository.findByClientIdAndStatutOrderByCreatedAtDesc(client.getId(), statut, pageable)
            : contratRepository.findByClientIdOrderByCreatedAtDesc(client.getId(), pageable);
        return page.map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContratResponseDto> getAllContrats(StatutContrat statut, Pageable pageable) {
        Page<Contrat> page = statut != null
            ? contratRepository.findByStatutOrderByCreatedAtDesc(statut, pageable)
            : contratRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ContratResponseDto getById(Long id, String userEmail, boolean isAdmin) {
        Contrat contrat = contratRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Contrat non trouvé : id=" + id));
        if (!isAdmin && !contrat.getClient().getEmail().equals(userEmail)) {
            throw new EntityNotFoundException("Contrat non trouvé");
        }
        return mapper.toDto(contrat);
    }

    @Override
    @Transactional
    public ContratResponseDto update(Long id, ContratUpdateRequestDto request) {
        log.info("Modification contrat : id={}", id);
        Contrat contrat = contratRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Contrat non trouvé : id=" + id));

        boolean isLocation = contrat.getTypeContrat() == TypeContrat.LOCATION;

        // ── Guardrails LOCATION ────────────────────────────────────────────────
        // montant et dateFin sont des valeurs dérivées : bloqués en écriture directe.
        // Utiliser dureeLocationMois pour recalculer les deux automatiquement.
        if (isLocation && request.montant() != null) {
            throw new IllegalArgumentException(
                "Le montant d'un contrat LOCATION est calculé automatiquement (loyer × durée). " +
                "Modifiez dureeLocationMois pour mettre à jour le montant.");
        }
        if (isLocation && request.dateFin() != null) {
            throw new IllegalArgumentException(
                "La date de fin d'un contrat LOCATION est calculée automatiquement (dateDebut + durée). " +
                "Modifiez dureeLocationMois pour mettre à jour la date de fin.");
        }
        if (isLocation && request.dureeLocationMois() != null && request.dureeLocationMois() <= 0) {
            throw new IllegalArgumentException("La durée doit être supérieure à 0 mois.");
        }

        // ── Guardrail VENTE ────────────────────────────────────────────────────
        if (!isLocation && request.dureeLocationMois() != null) {
            throw new IllegalArgumentException(
                "Le champ dureeLocationMois ne s'applique qu'aux contrats LOCATION.");
        }

        // ── Champs toujours modifiables ────────────────────────────────────────
        if (request.statut()      != null) contrat.setStatut(request.statut());
        if (request.documentUrl() != null) contrat.setDocumentUrl(request.documentUrl());
        if (request.notes()       != null) contrat.setNotes(request.notes());

        // ── Mise à jour VENTE (libre) ──────────────────────────────────────────
        if (!isLocation) {
            if (request.dateDebut() != null) contrat.setDateDebut(request.dateDebut());
            if (request.dateFin()   != null) contrat.setDateFin(request.dateFin());
            if (request.montant()   != null) contrat.setMontant(request.montant());
            return mapper.toDto(contratRepository.save(contrat));
        }

        // ── Mise à jour LOCATION (recalcul centralisé des champs dérivés) ───────
        // Fix NPE : Integer (non primitif) pour supporter dureeLocationMois null en base
        Integer effectiveDuree = request.dureeLocationMois() != null
            ? request.dureeLocationMois() : contrat.getDureeLocationMois();

        if (effectiveDuree == null) {
            // Contrat LOCATION sans dureeLocationMois en base : données corrompues
            throw new IllegalStateException(
                "Le contrat LOCATION #" + id + " n'a pas de dureeLocationMois en base. " +
                "Fournissez dureeLocationMois dans la requête pour recalculer.");
        }

        LocalDate effectiveDateDebut = request.dateDebut() != null
            ? request.dateDebut() : contrat.getDateDebut();

        if (request.dateDebut() != null) {
            contrat.setDateDebut(effectiveDateDebut);
        }
        if (request.dureeLocationMois() != null) {
            contrat.setDureeLocationMois(effectiveDuree);
            BigDecimal newMontant = montantLocation(contrat.getAnnonce().getPrix(), effectiveDuree);
            contrat.setMontant(newMontant);
            log.info("Durée contrat #{} → {} mois, montant recalculé = {} FCFA", id, effectiveDuree, newMontant);
        }
        if (request.dateDebut() != null || request.dureeLocationMois() != null) {
            contrat.setDateFin(dateFinLocation(effectiveDateDebut, effectiveDuree));
        }

        return mapper.toDto(contratRepository.save(contrat));
    }

    @Override
    @Transactional
    public ContratResponseDto demanderResiliation(Long id, ContratActionDto dto, String clientEmail) {
        Contrat contrat = loadClientContrat(id, clientEmail);
        if (contrat.getStatut() != StatutContrat.ACTIF) {
            throw new IllegalStateException(
                "Seul un contrat ACTIF peut faire l'objet d'une demande de résiliation. Statut actuel : " + contrat.getStatut()
            );
        }
        contrat.setStatut(StatutContrat.EN_ATTENTE_RESILIATION);
        if (dto.motif() != null) {
            contrat.setNotes("Demande résiliation client : " + dto.motif());
        }
        log.info("Demande de résiliation enregistrée : contratId={}, client={}", id, clientEmail);
        return mapper.toDto(contratRepository.save(contrat));
    }

    @Override
    @Transactional
    public ContratResponseDto demanderProlongation(Long id, ContratActionDto dto, String clientEmail) {
        Contrat contrat = loadClientContrat(id, clientEmail);
        if (contrat.getStatut() != StatutContrat.ACTIF) {
            throw new IllegalStateException(
                "Seul un contrat ACTIF peut faire l'objet d'une demande de prolongation. Statut actuel : " + contrat.getStatut()
            );
        }
        contrat.setStatut(StatutContrat.PROLONGATION_EN_ATTENTE);
        String note = dto.nouvelleDate() != null
            ? "Prolongation demandée jusqu'au " + dto.nouvelleDate() + (dto.motif() != null ? " — " + dto.motif() : "")
            : "Prolongation demandée" + (dto.motif() != null ? " — " + dto.motif() : "");
        contrat.setNotes(note);
        log.info("Demande de prolongation enregistrée : contratId={}, client={}", id, clientEmail);
        return mapper.toDto(contratRepository.save(contrat));
    }

    private Contrat loadClientContrat(Long id, String clientEmail) {
        var client = userRepository.findByEmail(clientEmail)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
        return contratRepository.findByIdAndClientId(id, client.getId())
            .orElseThrow(() -> new EntityNotFoundException("Contrat non trouvé pour ce client : id=" + id));
    }

    // ─── Calculs LOCATION centralisés ─────────────────────────────────────────
    // Source unique de vérité : toute modification des règles métier se fait ici.

    private BigDecimal montantLocation(BigDecimal loyerMensuel, int dureeEnMois) {
        return loyerMensuel.multiply(BigDecimal.valueOf(dureeEnMois));
    }

    private LocalDate dateFinLocation(LocalDate dateDebut, int dureeEnMois) {
        return dateDebut.plusMonths(dureeEnMois);
    }
}
