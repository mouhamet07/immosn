package sn.immosn.backend.client.web.annonce.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import sn.immosn.backend.annonce.data.entity.Commodite;
import sn.immosn.backend.client.web.annonce.dto.CommoditeRequestDto;
import sn.immosn.backend.client.web.annonce.dto.CommoditeResponseDto;

@Component
public class CommoditeMapper {

    public CommoditeResponseDto toResponse(Commodite entity) {
        if (entity == null) return null;
        return new CommoditeResponseDto(
                entity.getId(),
                entity.getLibelle()
        );
    }

    public Commodite toEntity(CommoditeRequestDto dto) {
        if (dto == null) return null;
        return Commodite.builder()
                .libelle(dto.libelle())
                .isArchived(false)
                .build();
    }

}