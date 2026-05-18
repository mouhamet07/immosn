package sn.immosn.backend.client.web.visite.mapper;

import org.springframework.stereotype.Component;
import sn.immosn.backend.client.web.visite.dto.DemandeVisiteResponseDto;
import sn.immosn.backend.visite.data.entity.DemandeVisite;

@Component
public class DemandeVisiteMapper {

    public DemandeVisiteResponseDto toDto(DemandeVisite v) {
        String image = (v.getAnnonce().getImages() != null && !v.getAnnonce().getImages().isEmpty())
            ? v.getAnnonce().getImages().get(0) : null;
        return new DemandeVisiteResponseDto(
            v.getId(),
            v.getClient().getId(),
            v.getClient().getNomComplet(),
            v.getAnnonce().getId(),
            v.getAnnonce().getLibelle(),
            v.getAnnonce().getAdresse(),
            image,
            v.getDateVisite(),
            v.getStatut(),
            v.getCommentaire(),
            v.getCreatedAt(),
            v.getUpdatedAt()
        );
    }
}
