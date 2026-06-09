package sn.immosn.backend.visite.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sn.immosn.backend.client.web.contrat.dto.ContratResponseDto;
import sn.immosn.backend.client.web.visite.dto.*;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;

public interface DemandeVisiteService {

    DemandeVisiteResponseDto create(DemandeVisiteCreateRequestDto request, String clientEmail);

    DemandeVisiteResponseDto getById(Long id, String userEmail, boolean isAdmin);

    Page<DemandeVisiteResponseDto> getClientVisites(String clientEmail, StatutDemandeVisite statut, Pageable pageable);

    Page<DemandeVisiteResponseDto> getAllVisites(StatutDemandeVisite statut, Pageable pageable);

    DemandeVisiteResponseDto updateStatut(Long id, UpdateStatutVisiteDto dto, String userEmail, boolean isAdmin);

    DemandeVisiteResponseDto updateDate(Long id, UpdateDateVisiteDto dto);

    DemandeVisiteResponseDto modifierParClient(Long id, UpdateDateVisiteDto dto, String clientEmail);

    void annuler(Long id, String clientEmail);

    /**
     * Clôture une visite ACCEPTEE.
     * SANS_SUITE : visite → CLOTUREE_SANS_SUITE, lead → ABANDONNE.
     * AVEC_CONTRAT : visite → CLOTUREE_AVEC_CONTRAT, contrat créé automatiquement, lead → CONVERTI.
     */
    ContratResponseDto cloturerVisite(Long id, CloturerVisiteDto dto);
}
