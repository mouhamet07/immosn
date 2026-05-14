package sn.immosn.backend.client.web.annonce.mapper;

import org.springframework.stereotype.Component;

import sn.immosn.backend.annonce.data.entity.TypeBienAnnonce;
import sn.immosn.backend.client.web.annonce.dto.TypeBienRequestDto;
import sn.immosn.backend.client.web.annonce.dto.TypeBienResponseDto;

@Component
public class TypeBienAnnonceMapper {

    public TypeBienResponseDto toResponseDto(TypeBienAnnonce typeBienAnnonce) {
        if (typeBienAnnonce == null) {
            return null;
        }
        return new TypeBienResponseDto(typeBienAnnonce.getId(), typeBienAnnonce.getLibelle());
    }

    public TypeBienAnnonce toEntity(TypeBienRequestDto request) {
        if (request == null) {
            return null;
        }
        return TypeBienAnnonce.builder()
                .libelle(request.libelle())
                .build();
    }
}
