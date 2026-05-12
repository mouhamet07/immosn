package sn.immosn.backend.annonce.service;

import java.util.List;

import sn.immosn.backend.client.web.annonce.dto.TypeBienRequestDto;
import sn.immosn.backend.client.web.annonce.dto.TypeBienResponseDto;

public interface TypeBienAnnonceService {
    TypeBienResponseDto createTypeBien(TypeBienRequestDto request);
    List<TypeBienResponseDto> getAllTypesBien();
    TypeBienResponseDto getTypeBienById(Long id);
    TypeBienResponseDto updateTypeBien(Long id, TypeBienRequestDto request);
    void archiveTypeBien(Long id);
}
