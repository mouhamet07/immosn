package sn.immosn.backend.visite.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sn.immosn.backend.client.web.visite.dto.*;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;

public interface DemandeVisiteService {

    DemandeVisiteResponseDto create(DemandeVisiteCreateRequestDto request, String clientEmail);

    Page<DemandeVisiteResponseDto> getClientVisites(String clientEmail, StatutDemandeVisite statut, Pageable pageable);

    Page<DemandeVisiteResponseDto> getAllVisites(StatutDemandeVisite statut, Pageable pageable);

    DemandeVisiteResponseDto updateStatut(Long id, UpdateStatutVisiteDto dto, String userEmail, boolean isAdmin);

    DemandeVisiteResponseDto updateDate(Long id, UpdateDateVisiteDto dto);

    void annuler(Long id, String clientEmail);
}
