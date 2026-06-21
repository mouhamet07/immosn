package sn.immosn.backend.proprietaire.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sn.immosn.backend.client.web.annonce.dto.AnnonceListDto;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireCreateRequestDto;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireResponseDto;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireStatsDto;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireUpdateRequestDto;

public interface ProprietaireService {

    ProprietaireResponseDto create(ProprietaireCreateRequestDto request);

    ProprietaireResponseDto update(Long id, ProprietaireUpdateRequestDto request);

    ProprietaireResponseDto getById(Long id);

    Page<ProprietaireResponseDto> getAll(Pageable pageable);

    ProprietaireStatsDto getStats(Long id);

    Page<AnnonceListDto> getBiens(Long id, Pageable pageable);
}
