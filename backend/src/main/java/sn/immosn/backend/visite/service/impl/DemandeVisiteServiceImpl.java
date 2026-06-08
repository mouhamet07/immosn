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
     * Transitions de statut.
     * CLIENT : peut uniquement passer à ANNULEE → lead → ABANDONNE.
     * ADMIN  : peut passer à ACCEPTEE ou REFUSEE (REFUSEE → lead → ABANDONNE).
     * La clôture (CLOTUREE_*) passe par cloturerVisite() — pas via cet endpoint.
     */
    @Override
    @Transactional
    public DemandeVisiteResponseDto updateStatut(Long id, UpdateStatutVisiteDto dto, String userEmail, boolean isAdmin) {
        DemandeVisite visite = loadVisite(id);

        if (!isAdmin) {
            if (!visite.getClient().getEmail().equals(userEmail)) {
                throw new EntityNotFoundException("Demande non trouvée");
            }
            if (dto.statut() != StatutDemandeVisite.ANNULEE) {
                throw new IllegalStateException("Le client peut seulement annuler une demande");
            }
        }

        visite.setStatut(dto.statut());
        if (dto.commentaire() != null) visite.setCommentaire(dto.commentaire());

        // Lead → ABANDONNE si la visite est refusée ou annulée via cet endpoint
        if (dto.statut() == StatutDemandeVisite.REFUSEE || dto.statut() == StatutDemandeVisite.ANNULEE) {
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
