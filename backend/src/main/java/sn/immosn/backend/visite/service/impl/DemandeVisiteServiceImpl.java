package sn.immosn.backend.visite.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.annonce.data.entity.TypeTransaction;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.auth.data.entity.RoleType;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.contrat.dto.ContratResponseDto;
import sn.immosn.backend.client.web.visite.dto.*;
import sn.immosn.backend.client.web.visite.mapper.DemandeVisiteMapper;
import sn.immosn.backend.contrat.data.entity.TypeContrat;
import sn.immosn.backend.contrat.service.ContratService;
import sn.immosn.backend.lead.data.entity.Lead;
import sn.immosn.backend.lead.data.entity.StatutLead;
import sn.immosn.backend.lead.data.repository.LeadRepository;
import sn.immosn.backend.lead.service.LeadHistoryService;
import sn.immosn.backend.prospect.data.entity.Prospect;
import sn.immosn.backend.prospect.data.repository.ProspectRepository;
import sn.immosn.backend.shared.exception.EntityNotFoundException;
import sn.immosn.backend.shared.service.VisiteTrackingNotificationService;
import sn.immosn.backend.visite.data.entity.DemandeVisite;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;
import sn.immosn.backend.visite.data.repository.DemandeVisiteRepository;
import sn.immosn.backend.visite.service.DemandeVisiteService;
import sn.immosn.backend.visite.service.VisiteHistoryService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemandeVisiteServiceImpl implements DemandeVisiteService {

    private static final Logger log = LoggerFactory.getLogger(DemandeVisiteServiceImpl.class);

    private final DemandeVisiteRepository visiteRepository;
    private final AnnonceRepository annonceRepository;
    private final UserRepository userRepository;
    private final ProspectRepository prospectRepository;
    private final LeadRepository leadRepository;
    private final DemandeVisiteMapper mapper;
    private final ContratService contratService;
    private final VisiteHistoryService visiteHistoryService;
    private final LeadHistoryService leadHistoryService;
    private final VisiteTrackingNotificationService visiteTrackingNotificationService;

    /**
     * Crée une demande de visite ET un lead automatiquement.
     * Le lead est créé dès la demande — avant même l'acceptation — pour tracer l'intérêt client.
     */
    @Override
    @Transactional
    public DemandeVisiteResponseDto create(DemandeVisiteCreateRequestDto request, String clientEmail) {
        User client = loadUser(clientEmail);
        Annonce annonce = annonceRepository.findByIdAndIsArchivedFalse(request.annonceId())
            .orElseThrow(() -> new EntityNotFoundException("Annonce non trouvée"));

        boolean visiteActiveExiste = visiteRepository.existsByClientIdAndAnnonceIdAndStatutIn(
            client.getId(), annonce.getId(),
            List.of(StatutDemandeVisite.EN_ATTENTE, StatutDemandeVisite.ACCEPTEE)
        );
        if (visiteActiveExiste) {
            throw new IllegalStateException(
                "Vous avez déjà une demande de visite active (EN_ATTENTE ou ACCEPTEE) pour ce bien. "
                + "Veuillez annuler ou attendre la clôture de votre demande existante avant d'en créer une nouvelle.");
        }

        DemandeVisite visite = DemandeVisite.builder()
            .client(client)
            .annonce(annonce)
            .dateVisite(request.dateVisite())
            .commentaire(request.commentaire())
            .build();

        visite = visiteRepository.save(visite);

        visiteHistoryService.record(visite, null, StatutDemandeVisite.EN_ATTENTE,
            "CREATION", request.commentaire(), null, null);

        // Lead créé automatiquement dès la demande de visite (pas à l'acceptation)
        boolean leadExisteDeja = !leadRepository.findByVisiteId(visite.getId()).isEmpty();
        if (!leadExisteDeja) {
            Lead lead = Lead.builder()
                .client(client)
                .annonce(annonce)
                .visite(visite)
                .build();
            Lead savedLead = leadRepository.save(lead);
            leadHistoryService.record(savedLead, null, StatutLead.EN_COURS,
                "CREATION", "Lead créé automatiquement pour visite #" + visite.getId());
            log.info("Lead auto-créé pour visite #{} (client={}, annonce={})", visite.getId(), client.getEmail(), annonce.getId());
        }

        return mapper.toDto(visite);
    }

    /**
     * Demande de visite d'un visiteur non authentifié.
     * Crée (ou réutilise par email) un Prospect, une DemandeVisite rattachée à ce prospect,
     * et un Lead (comme pour un client authentifié) pour permettre la conversion en contrat
     * sans attendre la création d'un compte.
     */
    @Override
    @Transactional
    public VisiteInviteResponseDto createInvite(VisiteInviteCreateRequestDto request) {
        Annonce annonce = annonceRepository.findByIdAndIsArchivedFalse(request.annonceId())
            .orElseThrow(() -> new EntityNotFoundException("Annonce non trouvée"));

        Prospect prospect = findOrCreateProspect(request);

        DemandeVisite visite = DemandeVisite.builder()
            .prospect(prospect)
            .annonce(annonce)
            .nom(request.nom())
            .prenom(request.prenom())
            .telephone(request.telephone())
            .email(request.email())
            .adresse(request.adresse())
            .dateVisite(request.dateVisite())
            .heureVisite(request.heureVisite())
            .commentaire(request.commentaire())
            .build();

        visite = visiteRepository.save(visite);
        visiteHistoryService.record(visite, null, StatutDemandeVisite.EN_ATTENTE,
            "CREATION_INVITE", request.commentaire(), null, null);

        boolean leadExisteDeja = !leadRepository.findByVisiteId(visite.getId()).isEmpty();
        if (!leadExisteDeja) {
            Lead lead = Lead.builder()
                .prospect(prospect)
                .annonce(annonce)
                .visite(visite)
                .build();
            Lead savedLead = leadRepository.save(lead);
            leadHistoryService.record(savedLead, null, StatutLead.EN_COURS,
                "CREATION", "Lead créé automatiquement pour visite invité #" + visite.getId());
        }

        log.info("Demande de visite invité #{} créée (prospect={}, annonce={})",
            visite.getId(), prospect.getEmail(), annonce.getId());

        visiteTrackingNotificationService.notifierNumeroSuivi(
            prospect.getNom(), prospect.getEmail(), prospect.getTelephone(), prospect.getToken());

        return mapper.toInviteDto(visite, prospect.getToken());
    }

    private Prospect findOrCreateProspect(VisiteInviteCreateRequestDto request) {
        return prospectRepository.findFirstByEmailOrderByCreatedAtAsc(request.email())
            .orElseGet(() -> prospectRepository.save(Prospect.builder()
                .nom(request.nom())
                .prenom(request.prenom())
                .email(request.email())
                .telephone(request.telephone())
                .adresse(request.adresse())
                .token(UUID.randomUUID().toString())
                .build()));
    }

    @Override
    @Transactional(readOnly = true)
    public SuiviVisiteResponseDto suivreParToken(String token) {
        Prospect prospect = prospectRepository.findByToken(token)
            .orElseThrow(() -> new EntityNotFoundException("Aucun suivi trouvé pour ce numéro."));
        List<DemandeVisiteResponseDto> visites = visiteRepository.findByProspectIdOrderByCreatedAtDesc(prospect.getId())
            .stream().map(mapper::toDto).toList();
        return new SuiviVisiteResponseDto(prospect.getPrenom() != null
            ? prospect.getPrenom() + " " + prospect.getNom() : prospect.getNom(), visites);
    }

    @Override
    @Transactional(readOnly = true)
    public DemandeVisiteResponseDto getById(Long id, String userEmail, boolean isAdmin) {
        DemandeVisite visite = visiteRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Demande de visite non trouvée : id=" + id));
        if (!isAdmin && !visite.getClient().getEmail().equals(userEmail)) {
            throw new EntityNotFoundException("Demande de visite non trouvée");
        }
        return mapper.toDto(visite);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DemandeVisiteResponseDto> getClientVisites(String clientEmail, StatutDemandeVisite statut, Pageable pageable) {
        User client = loadUser(clientEmail);
        Page<DemandeVisite> page = statut != null
            ? visiteRepository.findByClientIdAndStatutAndIsArchivedFalseOrderByCreatedAtDesc(client.getId(), statut, pageable)
            : visiteRepository.findByClientIdAndIsArchivedFalseOrderByCreatedAtDesc(client.getId(), pageable);
        return page.map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DemandeVisiteResponseDto> getAllVisites(
        StatutDemandeVisite statut, TypeTransaction typeTransaction, Pageable pageable
    ) {
        // Admin path: no isArchived filter — archived visits remain visible for audit/review.
        // Mutation helpers (loadVisite) still enforce isArchivedFalse to block edits on archived visits.
        Page<DemandeVisite> page;
        if (statut != null && typeTransaction != null) {
            page = visiteRepository.findByStatutAndAnnonce_TypeTransaction(statut, typeTransaction, pageable);
        } else if (statut != null) {
            page = visiteRepository.findByStatut(statut, pageable);
        } else if (typeTransaction != null) {
            page = visiteRepository.findByAnnonce_TypeTransaction(typeTransaction, pageable);
        } else {
            page = visiteRepository.findAll(pageable);
        }
        return page.map(mapper::toDto);
    }

    /**
     * Transitions de statut autorisées via cet endpoint.
     * CLIENT : EN_ATTENTE → ANNULEE  |  ACCEPTEE → ANNULEE  (lead → ABANDONNE).
     * ADMIN  : EN_ATTENTE → ACCEPTEE  |  EN_ATTENTE → REFUSEE  (REFUSEE → lead → ABANDONNE).
     * La clôture (CLOTUREE_*) passe exclusivement par cloturerVisite() — PUT /{id}/cloture.
     * TERMINEE est un statut historique en lecture seule : ne peut plus être défini via l'API.
     */
    @Override
    @Transactional
    @SuppressWarnings("deprecation")
    public DemandeVisiteResponseDto updateStatut(Long id, UpdateStatutVisiteDto dto, String userEmail, boolean isAdmin) {
        DemandeVisite visite = loadVisite(id);
        StatutDemandeVisite actuel = visite.getStatut();
        StatutDemandeVisite cible = dto.statut();

        // TERMINEE est en lecture seule — interdit en tant que cible via l'API
        if (cible == StatutDemandeVisite.TERMINEE) {
            throw new IllegalArgumentException(
                "TERMINEE est un statut historique en lecture seule. "
                + "Utilisez PUT /visites/{id}/cloture pour CLOTUREE_SANS_SUITE ou CLOTUREE_AVEC_CONTRAT.");
        }

        if (!isAdmin) {
            if (!visite.getClient().getEmail().equals(userEmail)) {
                throw new EntityNotFoundException("Demande non trouvée");
            }
            if (cible != StatutDemandeVisite.ANNULEE) {
                throw new IllegalStateException("Le client peut seulement annuler une demande.");
            }
            if (actuel != StatutDemandeVisite.EN_ATTENTE && actuel != StatutDemandeVisite.ACCEPTEE) {
                throw new IllegalStateException(
                    "Annulation impossible : la visite est déjà " + actuel + ".");
            }
        } else {
            // ADMIN : seules deux transitions sont autorisées via cet endpoint
            boolean transitionValide =
                (actuel == StatutDemandeVisite.EN_ATTENTE && cible == StatutDemandeVisite.ACCEPTEE) ||
                (actuel == StatutDemandeVisite.EN_ATTENTE && cible == StatutDemandeVisite.REFUSEE);
            if (!transitionValide) {
                throw new IllegalStateException(
                    "Transition invalide : " + actuel + " → " + cible
                    + ". Cet endpoint autorise uniquement EN_ATTENTE → ACCEPTEE ou EN_ATTENTE → REFUSEE. "
                    + "Pour clôturer une visite ACCEPTEE, utilisez PUT /visites/{id}/cloture.");
            }
        }

        visite.setStatut(cible);
        if (dto.commentaire() != null) visite.setCommentaire(dto.commentaire());

        // Lead → ABANDONNE si la visite est refusée ou annulée
        if (cible == StatutDemandeVisite.REFUSEE || cible == StatutDemandeVisite.ANNULEE) {
            abandonnerLeadsDeLaVisite(visite.getId());
        }

        DemandeVisite saved = visiteRepository.save(visite);

        String action = switch (cible) {
            case ACCEPTEE -> "ACCEPTATION";
            case REFUSEE -> "REFUS";
            case ANNULEE -> "ANNULATION";
            default -> "CHANGEMENT_STATUT";
        };
        visiteHistoryService.record(saved, actuel, cible, action, dto.commentaire(), null, null);

        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public DemandeVisiteResponseDto updateDate(Long id, UpdateDateVisiteDto dto) {
        DemandeVisite visite = loadVisite(id);
        LocalDateTime ancienneDate = visite.getDateVisite();
        visite.setDateVisite(dto.dateVisite());
        if (dto.commentaire() != null) visite.setCommentaire(dto.commentaire());
        DemandeVisite saved = visiteRepository.save(visite);
        visiteHistoryService.record(saved, saved.getStatut(), saved.getStatut(),
            "REPROGRAMMATION", dto.commentaire(), ancienneDate, dto.dateVisite());
        return mapper.toDto(saved);
    }

    /**
     * Modification client (EN_ATTENTE uniquement) : date et/ou commentaire.
     */
    @Override
    @Transactional
    public DemandeVisiteResponseDto modifierParClient(Long id, UpdateDateVisiteDto dto, String clientEmail) {
        DemandeVisite visite = loadVisite(id);
        if (!visite.getClient().getEmail().equals(clientEmail)) {
            throw new EntityNotFoundException("Demande non trouvée");
        }
        if (visite.getStatut() != StatutDemandeVisite.EN_ATTENTE) {
            throw new IllegalStateException(
                "Seule une visite EN_ATTENTE peut être modifiée. Statut actuel : " + visite.getStatut());
        }
        LocalDateTime ancienneDate = visite.getDateVisite();
        visite.setDateVisite(dto.dateVisite());
        if (dto.commentaire() != null) visite.setCommentaire(dto.commentaire());
        DemandeVisite saved = visiteRepository.save(visite);
        visiteHistoryService.record(saved, StatutDemandeVisite.EN_ATTENTE, StatutDemandeVisite.EN_ATTENTE,
            "REPROGRAMMATION_CLIENT", dto.commentaire(), ancienneDate, dto.dateVisite());
        return mapper.toDto(saved);
    }

    // Sprint 2 — processus commercial : affectation & replanification

    /**
     * Affecte un administrateur responsable à une visite ACCEPTEE.
     * Transition : ACCEPTEE → AFFECTEE. L'administrateur ciblé doit avoir le rôle ADMIN ou SUPER_ADMIN.
     */
    @Override
    @Transactional
    public DemandeVisiteResponseDto affecterAdmin(Long id, AffecterAdminDto dto) {
        DemandeVisite visite = loadVisite(id);
        StatutDemandeVisite actuel = visite.getStatut();
        if (actuel != StatutDemandeVisite.ACCEPTEE && actuel != StatutDemandeVisite.AFFECTEE) {
            throw new IllegalStateException(
                "Une visite doit être ACCEPTEE pour être affectée. Statut actuel : " + actuel);
        }
        User admin = userRepository.findById(dto.adminId())
            .orElseThrow(() -> new EntityNotFoundException("Administrateur non trouvé : id=" + dto.adminId()));
        boolean estAdmin = admin.getRoles().stream()
            .anyMatch(r -> r.getRole() == RoleType.ADMIN || r.getRole() == RoleType.SUPER_ADMIN);
        if (!estAdmin) {
            throw new IllegalArgumentException("L'utilisateur ciblé n'est pas un administrateur.");
        }

        visite.setAdminResponsable(admin);
        visite.setStatut(StatutDemandeVisite.AFFECTEE);
        if (dto.commentaire() != null) visite.setCommentaire(dto.commentaire());
        DemandeVisite saved = visiteRepository.save(visite);
        visiteHistoryService.record(saved, actuel, StatutDemandeVisite.AFFECTEE,
            "AFFECTATION", dto.commentaire(), null, null);
        log.info("Visite #{} affectée à l'admin #{} ({})", id, admin.getId(), admin.getEmail());
        return mapper.toDto(saved);
    }

    /**
     * Demande de replanification : mémorise la nouvelle date proposée et passe en REPLANIFICATION_DEMANDEE.
     * Autorisé depuis ACCEPTEE ou AFFECTEE.
     */
    @Override
    @Transactional
    public DemandeVisiteResponseDto demanderReplanification(Long id, ReplanificationDto dto) {
        DemandeVisite visite = loadVisite(id);
        StatutDemandeVisite actuel = visite.getStatut();
        if (actuel != StatutDemandeVisite.ACCEPTEE && actuel != StatutDemandeVisite.AFFECTEE) {
            throw new IllegalStateException(
                "Une replanification ne peut être demandée que pour une visite ACCEPTEE ou AFFECTEE. "
                + "Statut actuel : " + actuel);
        }
        visite.setDateReplanificationProposee(dto.nouvelleDate());
        visite.setStatut(StatutDemandeVisite.REPLANIFICATION_DEMANDEE);
        if (dto.commentaire() != null) visite.setCommentaire(dto.commentaire());
        DemandeVisite saved = visiteRepository.save(visite);
        visiteHistoryService.record(saved, actuel, StatutDemandeVisite.REPLANIFICATION_DEMANDEE,
            "REPLANIFICATION_DEMANDEE", dto.commentaire(), visite.getDateVisite(), dto.nouvelleDate());
        log.info("Replanification demandée pour visite #{} → {}", id, dto.nouvelleDate());
        return mapper.toDto(saved);
    }

    /**
     * Accepte la replanification : applique la date proposée et revient à l'état actif
     * (AFFECTEE si un responsable est assigné, sinon ACCEPTEE).
     */
    @Override
    @Transactional
    public DemandeVisiteResponseDto accepterReplanification(Long id) {
        DemandeVisite visite = loadVisite(id);
        if (visite.getStatut() != StatutDemandeVisite.REPLANIFICATION_DEMANDEE) {
            throw new IllegalStateException(
                "Aucune replanification en attente. Statut actuel : " + visite.getStatut());
        }
        LocalDateTime ancienneDate = visite.getDateVisite();
        LocalDateTime nouvelleDate = visite.getDateReplanificationProposee();
        if (nouvelleDate != null) {
            visite.setDateVisite(nouvelleDate);
        }
        visite.setDateReplanificationProposee(null);
        StatutDemandeVisite cible = visite.getAdminResponsable() != null
            ? StatutDemandeVisite.AFFECTEE : StatutDemandeVisite.ACCEPTEE;
        visite.setStatut(cible);
        DemandeVisite saved = visiteRepository.save(visite);
        visiteHistoryService.record(saved, StatutDemandeVisite.REPLANIFICATION_DEMANDEE, cible,
            "REPLANIFICATION_ACCEPTEE", null, ancienneDate, nouvelleDate);
        log.info("Replanification acceptée pour visite #{} → {} (statut {})", id, nouvelleDate, cible);
        return mapper.toDto(saved);
    }

    /**
     * Annulation client : visite → ANNULEE + lead → ABANDONNE.
     * La visite reste accessible (isArchived inchangé) pour conserver l'historique.
     */
    @Override
    @Transactional
    public void annuler(Long id, String clientEmail) {
        DemandeVisite visite = loadVisite(id);
        if (!visite.getClient().getEmail().equals(clientEmail)) {
            throw new EntityNotFoundException("Demande non trouvée");
        }
        StatutDemandeVisite actuel = visite.getStatut();
        if (actuel != StatutDemandeVisite.EN_ATTENTE && actuel != StatutDemandeVisite.ACCEPTEE) {
            throw new IllegalStateException("Annulation impossible : la visite est déjà " + actuel + ".");
        }
        visite.setStatut(StatutDemandeVisite.ANNULEE);
        visiteRepository.save(visite);
        visiteHistoryService.record(visite, actuel, StatutDemandeVisite.ANNULEE,
            "ANNULATION", null, null, null);
        abandonnerLeadsDeLaVisite(id);
    }

    /**
     * Clôture d'une visite ACCEPTEE par l'administrateur.
     * SANS_SUITE : visite → CLOTUREE_SANS_SUITE, lead → ABANDONNE, retourne null.
     * AVEC_CONTRAT : visite → CLOTUREE_AVEC_CONTRAT, contrat créé, lead → CONVERTI, retourne le ContratResponseDto.
     */
    @Override
    @Transactional
    public ContratResponseDto cloturerVisite(Long id, CloturerVisiteDto dto) {
        DemandeVisite visite = loadVisite(id);

        // Clôture possible depuis ACCEPTEE (flux Sprint 1), AFFECTEE ou RAPPORT_REDIGE (flux Sprint 2).
        StatutDemandeVisite actuel = visite.getStatut();
        if (actuel != StatutDemandeVisite.ACCEPTEE
                && actuel != StatutDemandeVisite.AFFECTEE
                && actuel != StatutDemandeVisite.RAPPORT_REDIGE) {
            throw new IllegalStateException(
                "Seule une visite ACCEPTEE, AFFECTEE ou avec RAPPORT_REDIGE peut être clôturée. "
                + "Statut actuel : " + actuel
            );
        }

        switch (dto.type()) {
            case SANS_SUITE -> {
                visite.setStatut(StatutDemandeVisite.CLOTUREE_SANS_SUITE);
                visiteRepository.save(visite);
                visiteHistoryService.record(visite, actuel,
                    StatutDemandeVisite.CLOTUREE_SANS_SUITE,
                    "CLOTURE_SANS_SUITE", null, null, null);
                abandonnerLeadsDeLaVisite(id);
                log.info("Visite #{} clôturée sans suite", id);
                return null;
            }
            case AVEC_CONTRAT -> {
                // Le type de contrat n'est plus choisi manuellement : il est imposé par le
                // TypeTransaction de l'annonce, fixé une fois pour toutes à sa création.
                TypeContrat typeContrat = visite.getAnnonce().getTypeTransaction() == TypeTransaction.LOCATION
                    ? TypeContrat.LOCATION : TypeContrat.VENTE;

                // Valider avant toute mutation d'état : un rollback protège,
                // mais la validation doit précéder la persistance.
                if (typeContrat == TypeContrat.LOCATION
                        && (dto.dureeLocationMois() == null || dto.dureeLocationMois() <= 0)) {
                    throw new IllegalArgumentException(
                        "dureeLocationMois est obligatoire et doit être > 0 pour un contrat LOCATION");
                }
                visite.setStatut(StatutDemandeVisite.CLOTUREE_AVEC_CONTRAT);
                visiteRepository.save(visite);
                visiteHistoryService.record(visite, actuel,
                    StatutDemandeVisite.CLOTUREE_AVEC_CONTRAT,
                    "CLOTURE_AVEC_CONTRAT", null, null, null);
                ContratResponseDto contrat = contratService.createFromVisite(visite, typeContrat, dto.dureeLocationMois());
                log.info("Visite #{} clôturée avec contrat #{}", id, contrat.id());
                return contrat;
            }
            default -> throw new IllegalArgumentException("Type de clôture inconnu : " + dto.type());
        }
    }

    // Helpers

    private void abandonnerLeadsDeLaVisite(Long visiteId) {
        List<Lead> leads = leadRepository.findByVisiteId(visiteId);
        leads.stream()
            .filter(l -> l.getStatut() == StatutLead.EN_COURS)
            .forEach(l -> {
                l.setStatut(StatutLead.ABANDONNE);
                leadRepository.save(l);
                leadHistoryService.record(l, StatutLead.EN_COURS, StatutLead.ABANDONNE,
                    "ABANDON", "Abandon automatique — visite #" + visiteId);
                log.info("Lead #{} → ABANDONNE (visite #{})", l.getId(), visiteId);
            });
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
    }

    private DemandeVisite loadVisite(Long id) {
        return visiteRepository.findByIdAndIsArchivedFalse(id)
            .orElseThrow(() -> new EntityNotFoundException("Demande de visite non trouvée avec l'ID: " + id));
    }
}
