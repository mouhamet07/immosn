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
import sn.immosn.backend.contrat.service.ContratHistoryService;
import sn.immosn.backend.contrat.service.ContratService;
import sn.immosn.backend.lead.data.entity.Lead;
import sn.immosn.backend.lead.data.entity.StatutLead;
import sn.immosn.backend.lead.data.repository.LeadRepository;
import sn.immosn.backend.lead.service.LeadHistoryService;
import sn.immosn.backend.shared.exception.EntityNotFoundException;
import sn.immosn.backend.shared.service.FileStorageService;
import sn.immosn.backend.visite.data.entity.DemandeVisite;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContratServiceImpl implements ContratService {

    private static final Logger log = LoggerFactory.getLogger(ContratServiceImpl.class);

    /**
     * Machine à états des contrats — source unique de vérité pour toutes les transitions.
     *
     * EN_ATTENTE              → ACTIF, RESILIE          (admin : activation ou rejet)
     * ACTIF                   → EXPIRE, EN_ATTENTE_RESILIATION, PROLONGATION_EN_ATTENTE
     *                           (respectivement : job, demande client, demande client)
     * EN_ATTENTE_RESILIATION  → RESILIE, ACTIF          (admin : acceptation ou refus)
     * PROLONGATION_EN_ATTENTE → ACTIF                   (admin : acceptation ou refus)
     * EXPIRE, RESILIE         → (aucune) — états finaux
     *
     * Le job ContratExpirationJob applique ACTIF → EXPIRE directement sur les entités
     * sans passer par cette validation (bypass assumé, cohérent avec la machine à états).
     */
    private static final Map<StatutContrat, Set<StatutContrat>> TRANSITIONS_AUTORISEES = Map.of(
        StatutContrat.EN_ATTENTE,              Set.of(StatutContrat.ACTIF, StatutContrat.RESILIE),
        StatutContrat.ACTIF,                   Set.of(StatutContrat.EXPIRE,
                                                      StatutContrat.EN_ATTENTE_RESILIATION,
                                                      StatutContrat.PROLONGATION_EN_ATTENTE),
        StatutContrat.EN_ATTENTE_RESILIATION,  Set.of(StatutContrat.RESILIE, StatutContrat.ACTIF),
        StatutContrat.PROLONGATION_EN_ATTENTE, Set.of(StatutContrat.ACTIF),
        StatutContrat.EXPIRE,                  Set.of(),
        StatutContrat.RESILIE,                 Set.of()
    );

    private final ContratRepository      contratRepository;
    private final UserRepository         userRepository;
    private final AnnonceRepository      annonceRepository;
    private final LeadRepository         leadRepository;
    private final ContratMapper          mapper;
    private final ContratHistoryService  contratHistoryService;
    private final LeadHistoryService     leadHistoryService;
    private final FileStorageService     fileStorageService;

    @Override
    @Transactional
    public ContratResponseDto create(ContratCreateRequestDto request) {
        log.info("Création contrat : clientId={}, annonceId={}", request.clientId(), request.annonceId());

        var client  = userRepository.findById(request.clientId())
            .orElseThrow(() -> new EntityNotFoundException("Client non trouvé : id=" + request.clientId()));
        var annonce = annonceRepository.findByIdAndIsArchivedFalse(request.annonceId())
            .orElseThrow(() -> new EntityNotFoundException("Annonce non trouvée : id=" + request.annonceId()));

        if (request.dateDebut() == null) {
            throw new IllegalArgumentException("La date de début est obligatoire.");
        }
        if (request.dateFin() != null && !request.dateFin().isAfter(request.dateDebut())) {
            throw new IllegalArgumentException("La date de fin doit être strictement postérieure à la date de début.");
        }
        // Un lead ne peut être converti que si le type de contrat est renseigné
        if (request.leadId() != null && request.typeContrat() == null) {
            throw new IllegalArgumentException(
                "typeContrat est obligatoire lors de la conversion d'un lead en contrat (VENTE ou LOCATION).");
        }

        Contrat.ContratBuilder builder = Contrat.builder()
            .client(client)
            .annonce(annonce)
            .typeContrat(request.typeContrat())
            .dateDebut(request.dateDebut())
            .dateFin(request.dateFin())
            .montant(request.montant())
            .documentUrl(request.documentUrl())
            .notes(request.notes());

        if (request.leadId() != null) {
            var lead = leadRepository.findById(request.leadId())
                .orElseThrow(() -> new EntityNotFoundException("Lead non trouvé : id=" + request.leadId()));

            // INVARIANT : si le lead a une visite associée, la création du contrat doit obligatoirement
            // passer par cloturerVisite(AVEC_CONTRAT) qui garantit la cohérence visite→lead→contrat
            // dans une seule transaction avec enregistrement complet des historiques.
            if (lead.getVisite() != null) {
                throw new IllegalStateException(
                    "Le lead #" + lead.getId() + " est lié à la visite #" + lead.getVisite().getId()
                    + ". La création d'un contrat doit passer par la clôture de la visite"
                    + " (PUT /api/v1/visites/" + lead.getVisite().getId() + "/cloture avec type=AVEC_CONTRAT)"
                    + " afin de garantir la cohérence visite → lead → contrat.");
            }

            builder.lead(lead);
            StatutLead ancienStatutLead = lead.getStatut();
            lead.setStatut(StatutLead.CONVERTI);
            lead.setConvertedAt(LocalDateTime.now());
            leadRepository.save(lead);
            leadHistoryService.record(lead, ancienStatutLead, StatutLead.CONVERTI,
                "CONVERSION", "Conversion lors de la création du contrat");
        }

        Contrat saved = contratRepository.save(builder.build());
        log.info("Contrat créé : id={}", saved.getId());
        contratHistoryService.record(saved, null, StatutContrat.EN_ATTENTE, "CREATION", null);
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
        if (leads.size() > 1) {
            throw new IllegalStateException(
                "Incohérence de données : " + leads.size() + " leads liés à la visite #" + visite.getId()
                + ". Résolvez la duplication de leads avant de clôturer cette visite.");
        }
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
            StatutLead ancienStatutLead = lead.getStatut();
            lead.setStatut(StatutLead.CONVERTI);
            lead.setConvertedAt(LocalDateTime.now());
            leadRepository.save(lead);
            leadHistoryService.record(lead, ancienStatutLead, StatutLead.CONVERTI,
                "CONVERSION", "Conversion automatique lors de la clôture visite #" + visite.getId());
        }

        Contrat saved = contratRepository.save(builder.build());
        log.info("Contrat auto-créé depuis visite #{} : contratId={}, type={}", visite.getId(), saved.getId(), typeContrat);
        contratHistoryService.record(saved, null, StatutContrat.EN_ATTENTE,
            "CREATION_AUTO_VISITE", "Contrat créé automatiquement depuis la visite #" + visite.getId());
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

        StatutContrat ancienStatut = contrat.getStatut();
        boolean isLocation = contrat.getTypeContrat() == TypeContrat.LOCATION;

        // Guardrails LOCATION : montant et dateFin sont des valeurs dérivées, bloqués en écriture directe.
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

        // Guardrail VENTE
        if (!isLocation && request.dureeLocationMois() != null) {
            throw new IllegalArgumentException(
                "Le champ dureeLocationMois ne s'applique qu'aux contrats LOCATION.");
        }

        // Champs toujours modifiables
        if (request.statut() != null) {
            // Ces statuts sont réservés aux endpoints dédiés ou au job système — bloqués en modification directe
            if (request.statut() == StatutContrat.EN_ATTENTE_RESILIATION
                    || request.statut() == StatutContrat.PROLONGATION_EN_ATTENTE
                    || request.statut() == StatutContrat.EXPIRE) {
                throw new IllegalArgumentException(
                    "Le statut " + request.statut() + " ne peut pas être défini directement. "
                    + "EXPIRE est réservé au job système quotidien. "
                    + "Utilisez les endpoints /resiliation ou /prolongation pour les autres statuts protégés.");
            }
            validateTransition(contrat.getStatut(), request.statut());
            contrat.setStatut(request.statut());
        }
        if (request.documentUrl() != null) contrat.setDocumentUrl(request.documentUrl());
        if (request.notes()       != null) contrat.setNotes(request.notes());

        // Mise à jour VENTE
        if (!isLocation) {
            if (request.dateDebut() != null) contrat.setDateDebut(request.dateDebut());
            if (request.dateFin()   != null) contrat.setDateFin(request.dateFin());
            if (contrat.getDateFin() != null && !contrat.getDateFin().isAfter(contrat.getDateDebut())) {
                throw new IllegalArgumentException("La date de fin doit être strictement postérieure à la date de début.");
            }
            if (request.montant()   != null) contrat.setMontant(request.montant());
            Contrat saved = contratRepository.save(contrat);
            if (request.statut() != null && !ancienStatut.equals(saved.getStatut())) {
                contratHistoryService.record(saved, ancienStatut, saved.getStatut(),
                    resolveUpdateAction(ancienStatut, saved.getStatut()), null);
            }
            return mapper.toDto(saved);
        }

        // Mise à jour LOCATION — recalcul centralisé des champs dérivés
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

        Contrat saved = contratRepository.save(contrat);
        if (request.statut() != null && !ancienStatut.equals(saved.getStatut())) {
            contratHistoryService.record(saved, ancienStatut, saved.getStatut(),
                resolveUpdateAction(ancienStatut, saved.getStatut()), null);
        }
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public ContratResponseDto demanderResiliation(Long id, ContratActionDto dto, String clientEmail) {
        Contrat contrat = loadClientContrat(id, clientEmail);
        validateTransition(contrat.getStatut(), StatutContrat.EN_ATTENTE_RESILIATION);
        contrat.setStatut(StatutContrat.EN_ATTENTE_RESILIATION);
        contrat.setMotifResiliation(dto.motif());
        Contrat saved = contratRepository.save(contrat);
        log.info("Demande de résiliation enregistrée : contratId={}, client={}", id, clientEmail);
        contratHistoryService.record(saved, StatutContrat.ACTIF, StatutContrat.EN_ATTENTE_RESILIATION,
            "DEMANDE_RESILIATION", dto.motif());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public ContratResponseDto demanderProlongation(Long id, ContratActionDto dto, String clientEmail) {
        Contrat contrat = loadClientContrat(id, clientEmail);
        validateTransition(contrat.getStatut(), StatutContrat.PROLONGATION_EN_ATTENTE);
        contrat.setStatut(StatutContrat.PROLONGATION_EN_ATTENTE);
        String motifProlong = dto.nouvelleDate() != null
            ? "Prolongation demandée jusqu'au " + dto.nouvelleDate() + (dto.motif() != null ? " — " + dto.motif() : "")
            : (dto.motif() != null ? dto.motif() : null);
        contrat.setMotifProlongation(motifProlong);
        Contrat saved = contratRepository.save(contrat);
        log.info("Demande de prolongation enregistrée : contratId={}, client={}", id, clientEmail);
        contratHistoryService.record(saved, StatutContrat.ACTIF, StatutContrat.PROLONGATION_EN_ATTENTE,
            "DEMANDE_PROLONGATION", motifProlong);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public ContratResponseDto accepterResiliation(Long id, ContratActionDto dto) {
        Contrat contrat = contratRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Contrat non trouvé : id=" + id));
        validateTransition(contrat.getStatut(), StatutContrat.RESILIE);
        contrat.setStatut(StatutContrat.RESILIE);
        String motif = null;
        if (dto != null && dto.motif() != null && !dto.motif().isBlank()) {
            motif = dto.motif();
            String note     = "[Admin] Résiliation acceptée — " + motif;
            String existing = contrat.getNotes();
            contrat.setNotes(existing != null && !existing.isBlank() ? existing + "\n" + note : note);
        }
        Contrat saved = contratRepository.save(contrat);
        log.info("Résiliation acceptée : contratId={}", id);
        contratHistoryService.record(saved, StatutContrat.EN_ATTENTE_RESILIATION, StatutContrat.RESILIE,
            "ACCEPTATION_RESILIATION", motif);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public ContratResponseDto refuserResiliation(Long id, ContratActionDto dto) {
        Contrat contrat = contratRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Contrat non trouvé : id=" + id));
        validateTransition(contrat.getStatut(), StatutContrat.ACTIF);
        contrat.setStatut(StatutContrat.ACTIF);
        String motif = null;
        if (dto != null && dto.motif() != null && !dto.motif().isBlank()) {
            motif = dto.motif();
            String note     = "[Admin] Résiliation refusée — " + motif;
            String existing = contrat.getNotes();
            contrat.setNotes(existing != null && !existing.isBlank() ? existing + "\n" + note : note);
        }
        Contrat saved = contratRepository.save(contrat);
        log.info("Résiliation refusée : contratId={}", id);
        contratHistoryService.record(saved, StatutContrat.EN_ATTENTE_RESILIATION, StatutContrat.ACTIF,
            "REFUS_RESILIATION", motif);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public ContratResponseDto accepterProlongation(Long id, ContratActionDto dto) {
        Contrat contrat = contratRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Contrat non trouvé : id=" + id));
        validateTransition(contrat.getStatut(), StatutContrat.ACTIF);

        if (dto != null && dto.nouvelleDate() != null) {
            LocalDate nouvelleDate = dto.nouvelleDate();
            if (contrat.getTypeContrat() == TypeContrat.LOCATION) {
                long nouvelleDureeL = ChronoUnit.MONTHS.between(contrat.getDateDebut(), nouvelleDate);
                if (nouvelleDureeL <= 0) {
                    throw new IllegalArgumentException(
                        "La nouvelle date de fin doit être postérieure à la date de début du contrat.");
                }
                if (contrat.getDureeLocationMois() != null && nouvelleDureeL <= contrat.getDureeLocationMois()) {
                    throw new IllegalArgumentException(
                        "La nouvelle durée (" + nouvelleDureeL + " mois) doit être supérieure à la durée actuelle ("
                        + contrat.getDureeLocationMois() + " mois) pour une prolongation.");
                }
                int nouvelleDuree = (int) nouvelleDureeL;
                contrat.setDureeLocationMois(nouvelleDuree);
                contrat.setDateFin(nouvelleDate);
                contrat.setMontant(montantLocation(contrat.getAnnonce().getPrix(), nouvelleDuree));
                log.info("Prolongation LOCATION #{} : {}mois, dateFin={}, montant={} FCFA",
                    id, nouvelleDuree, nouvelleDate, contrat.getMontant());
            } else {
                contrat.setDateFin(nouvelleDate);
            }
        }

        contrat.setStatut(StatutContrat.ACTIF);
        String motif = null;
        if (dto != null && dto.motif() != null && !dto.motif().isBlank()) {
            motif = dto.motif();
            String note     = "[Admin] Prolongation acceptée — " + motif;
            String existing = contrat.getNotes();
            contrat.setNotes(existing != null && !existing.isBlank() ? existing + "\n" + note : note);
        }
        Contrat saved = contratRepository.save(contrat);
        log.info("Prolongation acceptée : contratId={}", id);
        contratHistoryService.record(saved, StatutContrat.PROLONGATION_EN_ATTENTE, StatutContrat.ACTIF,
            "ACCEPTATION_PROLONGATION", motif);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public ContratResponseDto refuserProlongation(Long id, ContratActionDto dto) {
        Contrat contrat = contratRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Contrat non trouvé : id=" + id));
        validateTransition(contrat.getStatut(), StatutContrat.ACTIF);
        contrat.setStatut(StatutContrat.ACTIF);
        String motif = null;
        if (dto != null && dto.motif() != null && !dto.motif().isBlank()) {
            motif = dto.motif();
            String note     = "[Admin] Prolongation refusée — " + motif;
            String existing = contrat.getNotes();
            contrat.setNotes(existing != null && !existing.isBlank() ? existing + "\n" + note : note);
        }
        Contrat saved = contratRepository.save(contrat);
        log.info("Prolongation refusée : contratId={}", id);
        contratHistoryService.record(saved, StatutContrat.PROLONGATION_EN_ATTENTE, StatutContrat.ACTIF,
            "REFUS_PROLONGATION", motif);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public ContratResponseDto uploadDocument(Long id, MultipartFile file) {
        Contrat contrat = contratRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Contrat non trouvé : id=" + id));
        String existingUrl = contrat.getDocumentUrl();
        if (existingUrl != null && existingUrl.startsWith("/uploads/")) {
            fileStorageService.delete(existingUrl);
        }
        String url = fileStorageService.store(file, "contrats");
        contrat.setDocumentUrl(url);
        Contrat saved = contratRepository.save(contrat);
        log.info("Document uploadé pour contrat #{} : {}", id, url);
        return mapper.toDto(saved);
    }

    private Contrat loadClientContrat(Long id, String clientEmail) {
        var client = userRepository.findByEmail(clientEmail)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
        return contratRepository.findByIdAndClientId(id, client.getId())
            .orElseThrow(() -> new EntityNotFoundException("Contrat non trouvé pour ce client : id=" + id));
    }

    // Machine à états

    private void validateTransition(StatutContrat actuel, StatutContrat cible) {
        Set<StatutContrat> permis = TRANSITIONS_AUTORISEES.getOrDefault(actuel, Set.of());
        if (!permis.contains(cible)) {
            String autorisees = permis.isEmpty() ? "aucune — état final" : permis.toString();
            throw new IllegalStateException(
                "Transition de statut invalide : " + actuel + " → " + cible
                + ". Transitions autorisées depuis " + actuel + " : " + autorisees + ".");
        }
    }

    private String resolveUpdateAction(StatutContrat ancien, StatutContrat nouveau) {
        if (ancien == StatutContrat.EN_ATTENTE && nouveau == StatutContrat.ACTIF)    return "VALIDATION";
        if (ancien == StatutContrat.EN_ATTENTE && nouveau == StatutContrat.RESILIE)  return "REJET";
        return "CHANGEMENT_STATUT";
    }

    // Calculs LOCATION

    private BigDecimal montantLocation(BigDecimal loyerMensuel, int dureeEnMois) {
        return loyerMensuel.multiply(BigDecimal.valueOf(dureeEnMois));
    }

    private LocalDate dateFinLocation(LocalDate dateDebut, int dureeEnMois) {
        return dateDebut.plusMonths(dureeEnMois);
    }
}
