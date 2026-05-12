package sn.immosn.backend.annonce.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import sn.immosn.backend.client.web.annonce.dto.CommoditeRequestDto;
import sn.immosn.backend.client.web.annonce.dto.CommoditeResponseDto;

public interface CommoditeService {
    CommoditeResponseDto createCommodite(CommoditeRequestDto request);
    List<CommoditeResponseDto> getAllCommodites();             // actives seulement
    List<CommoditeResponseDto> getAllCommoditesPaged(Pageable pageable);
    CommoditeResponseDto getCommoditeById(Long id);
    CommoditeResponseDto updateCommodite(Long id, CommoditeRequestDto request);
    void archiveCommodite(Long id);
}
