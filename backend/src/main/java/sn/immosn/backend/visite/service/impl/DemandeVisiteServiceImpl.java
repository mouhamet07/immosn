package sn.immosn.backend.visite.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.visite.dto.*;
import sn.immosn.backend.client.web.visite.mapper.DemandeVisiteMapper;
import sn.immosn.backend.lead.data.entity.Lead;
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

    private final DemandeVisiteRepository visiteRepository;
    private final AnnonceRepository annonceRepository;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final DemandeVisiteMapper mapper;

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

        return mapper.toDto(visiteRepository.save(visite));
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

        // Créer automatiquement un lead quand la visite est acceptée
        if (dto.statut() == StatutDemandeVisite.ACCEPTEE) {
            boolean leadExists = !leadRepository.findByVisiteId(visite.getId()).isEmpty();
            if (!leadExists) {
                Lead lead = Lead.builder()
                    .client(visite.getClient())
                    .annonce(visite.getAnnonce())
                    .visite(visite)
                    .build();
                leadRepository.save(lead);
            }
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
