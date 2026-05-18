package sn.immosn.backend.contrat.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sn.immosn.backend.client.web.contrat.dto.*;
import sn.immosn.backend.contrat.data.entity.StatutContrat;

public interface ContratService {
    ContratResponseDto create(ContratCreateRequestDto request);
    Page<ContratResponseDto> getClientContrats(String clientEmail, StatutContrat statut, Pageable pageable);
    Page<ContratResponseDto> getAllContrats(StatutContrat statut, Pageable pageable);
    ContratResponseDto getById(Long id, String userEmail, boolean isAdmin);
    ContratResponseDto update(Long id, ContratUpdateRequestDto request);
    ContratResponseDto demanderResiliation(Long id, ContratActionDto dto, String clientEmail);
    ContratResponseDto demanderProlongation(Long id, ContratActionDto dto, String clientEmail);
}
