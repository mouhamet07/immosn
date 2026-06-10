package sn.immosn.backend.annonce.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import sn.immosn.backend.client.web.annonce.dto.AnnonceCreateRequestDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceListDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceResponseDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceUpdateRequestDto;
import sn.immosn.backend.client.web.annonce.dto.SearchAnnonceRequestDto;

public interface AnnonceService {
    // Création
    AnnonceResponseDto createAnnonce(AnnonceCreateRequestDto request);

    // Lecture
    AnnonceResponseDto getAnnonceById(Long id);
    Page<AnnonceListDto> getAllAnnonces(Pageable pageable);

    // Mise à jour
    AnnonceResponseDto updateAnnonce(Long id, AnnonceUpdateRequestDto request);

    // Soft delete
    void archiveAnnonce(Long id);    // archived = true  (DELETE logique)
    void restoreAnnonce(Long id);    // archived = false (optionnel Sprint 1)

    // Recherche avancée (Sprint 2)
    Page<AnnonceListDto> searchAnnonces(SearchAnnonceRequestDto request);

    // Admin : liste incluant archivées
    Page<AnnonceListDto> getAllAnnoncesAdmin(Pageable pageable);
    AnnonceResponseDto getAnnonceByIdAdmin(Long id);
}
