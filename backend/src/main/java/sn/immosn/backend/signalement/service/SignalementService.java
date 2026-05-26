package sn.immosn.backend.signalement.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sn.immosn.backend.client.web.signalement.dto.*;
import sn.immosn.backend.signalement.data.entity.StatutSignalement;

public interface SignalementService {
    SignalementResponseDto create(SignalementCreateRequestDto request, String clientEmail);
    Page<SignalementResponseDto> getClientSignalements(String clientEmail, StatutSignalement statut, Pageable pageable);
    Page<SignalementResponseDto> getAll(StatutSignalement statut, Pageable pageable);
    SignalementResponseDto updateStatut(Long id, UpdateStatutSignalementDto dto);
    void markAsRead(Long id);
}
