package sn.immosn.backend.client.web.annonce.mapper;

import org.springframework.stereotype.Component;

import sn.immosn.backend.annonce.data.entity.Commodite;
import sn.immosn.backend.client.web.annonce.dto.CommoditeRequestDto;
import sn.immosn.backend.client.web.annonce.dto.CommoditeResponseDto;

@Component
public class CommoditeMapper {

    public CommoditeResponseDto toResponseDto(Commodite commodite) {
        if (commodite == null) {
            return null;
        }
        return new CommoditeResponseDto(commodite.getId(), commodite.getLibelle());
    }

    public Commodite toEntity(CommoditeRequestDto request) {
        if (request == null) {
            return null;
        }
        return Commodite.builder()
            .libelle(request.libelle())
            .build();
    }
}
