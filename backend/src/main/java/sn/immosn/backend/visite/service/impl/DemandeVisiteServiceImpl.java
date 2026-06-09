package sn.immosn.backend.visite.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
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
import sn.immosn.backend.shared.exception.EntityNotFoundException;
import sn.immosn.backend.visite.data.entity.DemandeVisite;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;
import sn.immosn.backend.visite.data.repository.DemandeVisiteRepository;
import sn.immosn.backend.visite.service.DemandeVisiteService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DemandeVisiteServiceImpl implements DemandeVisiteService {

    private static final Logger log = LoggerFactory.getLogger(DemandeVisiteServiceImpl.class);

    private final DemandeVisiteRepository visiteRepository;
    private final AnnonceRepository       annonceRepository;
    private final UserRepository          userRepository;
    private final LeadRepository          leadRepository;
    private final DemandeVisiteMapper     mapper;
    private final ContratService          contratService;

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

        // Lead créé automatiquement dès la demande de visite (pas à l'acceptation)
        boolean leadExisteDeja = !leadRepository.findByVisiteId(visite.getId()).isEmpty();
        if (!leadExisteDeja) {
            Lead lead = Lead.builder()
                .client(client)
                .annonce(annonce)
                .visite(visite)
                .build();
            leadRepository.save(lead);
            log.info("Lead auto-créé pour visite #{} (client={}, annonce={})", visite.getId(), client.getEmail(), annonce.getId());
        }

        return mapper.toDto(visite);
    }

    @Override
    @Transactional(readOnly = true)
    public DemandeVisiteResponseDto getById(Long id, String userEmail, boolean isAdmin) {
        DemandeVisite visite = visiteRepository.findByIdAndIsArchivedFalse(id)
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
    public Page<DemandeVisiteResponseDto> getAllVisites(StatutDemandeVisite statut, Pageable pageable) {
        Page<DemandeVisite> page = statut != null
            ? visiteRepository.findByStatutAndIsArchivedFalseOrderByCreatedAtDesc(statut, pageable)
            : visiteRepository.findByIsArchivedFalseOrderByCreatedAtDesc(pageable);
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
        StatutDemandeVisite cible  = dto.statut();

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
        // ANNULEE : archiver pour masquer des listes — identique au comportement DELETE /visites/{id}
        if (cible == StatutDemandeVisite.ANNULEE) {
            visite.setArchived(true);
        }

        // Lead → ABANDONNE si la visite est refusée ou annulée
        if (cible == StatutDemandeVisite.REFUSEE || cible == StatutDemandeVisite.ANNULEE) {
            abandonnerLeadsDeLaVisite(visite.getId());
        }

        return mapper.toDto(visiteRepository.save(visite));
    }

    @Override
    @Transactional
    public DemandeVisiteResponseDto updateDate(Long id, UpdateDateVisiteDto dto) {
        DemandeVisite visite = loadVisite(id);
        visite.setDateVisite(dto.dateVisite());
        if (dto.commentaire() != null) visite.setCommentaire(dto.commentaire());
        return mapper.toDto(visiteRepository.save(visite));
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
        visite.setDateVisite(dto.dateVisite());
        if (dto.commentaire() != null) visite.setCommentaire(dto.commentaire());
        return mapper.toDto(visiteRepository.save(visite));
    }

    /**
     * Annulation client : visite → ANNULEE + isArchived=true + lead → ABANDONNE.
     */
    @Override
    @Transactional
    public void annuler(Long id, String clientEmail) {
        DemandeVisite visite = loadVisite(id);
        if (!visite.getClient().getEmail().equals(clientEmail)) {
            throw new EntityNotFoundException("Demande non trouvée");
        }
        visite.setStatut(StatutDemandeVisite.ANNULEE);
        visite.setArchived(true);
        visiteRepository.save(visite);
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

        if (visite.getStatut() != StatutDemandeVisite.ACCEPTEE) {
            throw new IllegalStateException(
                "Seule une visite ACCEPTÉE peut être clôturée. Statut actuel : " + visite.getStatut()
            );
        }

        switch (dto.type()) {
            case SANS_SUITE -> {
                visite.setStatut(StatutDemandeVisite.CLOTUREE_SANS_SUITE);
                visiteRepository.save(visite);
                abandonnerLeadsDeLaVisite(id);
                log.info("Visite #{} clôturée sans suite", id);
                return null;
            }
            case AVEC_CONTRAT -> {
                // Valider avant toute mutation d'état : un rollback protège,
                // mais la validation doit précéder la persistance.
                if (dto.typeContrat() == null) {
                    throw new IllegalArgumentException(
                        "typeContrat est obligatoire pour une clôture AVEC_CONTRAT (VENTE ou LOCATION)");
                }
                if (dto.typeContrat() == TypeContrat.LOCATION
                        && (dto.dureeLocationMois() == null || dto.dureeLocationMois() <= 0)) {
                    throw new IllegalArgumentException(
                        "dureeLocationMois est obligatoire et doit être > 0 pour un contrat LOCATION");
                }
                visite.setStatut(StatutDemandeVisite.CLOTUREE_AVEC_CONTRAT);
                visiteRepository.save(visite);
                ContratResponseDto contrat = contratService.createFromVisite(visite, dto.typeContrat(), dto.dureeLocationMois());
                log.info("Visite #{} clôturée avec contrat #{}", id, contrat.id());
                return contrat;
            }
            default -> throw new IllegalArgumentException("Type de clôture inconnu : " + dto.type());
        }
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private void abandonnerLeadsDeLaVisite(Long visiteId) {
        List<Lead> leads = leadRepository.findByVisiteId(visiteId);
        leads.stream()
            .filter(l -> l.getStatut() == StatutLead.EN_COURS)
            .forEach(l -> {
                l.setStatut(StatutLead.ABANDONNE);
                leadRepository.save(l);
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
