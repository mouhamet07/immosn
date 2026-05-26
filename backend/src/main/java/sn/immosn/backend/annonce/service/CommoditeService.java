package sn.immosn.backend.annonce.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import sn.immosn.backend.client.web.annonce.dto.CommoditeRequestDto;
import sn.immosn.backend.client.web.annonce.dto.CommoditeResponseDto;

public interface CommoditeService {
    CommoditeResponseDto createCommodite(CommoditeRequestDto request);
    List<CommoditeResponseDto> getAllCommodites();             // actives seulement pour les clients
    Page<CommoditeResponseDto> getAllCommoditesPaged(Pageable pageable); // pour l'admin, inclut archivées
    CommoditeResponseDto getCommoditeById(Long id);
    CommoditeResponseDto updateCommodite(Long id, CommoditeRequestDto request);
    void archiveCommodite(Long id);
    CommoditeResponseDto restoreCommodite(Long id);
}
