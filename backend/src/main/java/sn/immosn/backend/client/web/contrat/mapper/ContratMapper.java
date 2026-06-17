package sn.immosn.backend.client.web.contrat.mapper;

import org.springframework.stereotype.Component;
import sn.immosn.backend.client.web.contrat.dto.ContratResponseDto;
import sn.immosn.backend.contrat.data.entity.Contrat;

@Component
public class ContratMapper {

    public ContratResponseDto toDto(Contrat c) {
        var annonce = c.getAnnonce();
        String image = (annonce != null && annonce.getImages() != null && !annonce.getImages().isEmpty())
            ? annonce.getImages().get(0) : null;

        Long visiteId = (c.getLead() != null && c.getLead().getVisite() != null)
            ? c.getLead().getVisite().getId() : null;

        return new ContratResponseDto(
            c.getId(),
            c.getClient().getId(),
            c.getClient().getNomComplet(),
            annonce != null ? annonce.getId() : null,
            annonce != null ? annonce.getLibelle() : null,
            annonce != null ? annonce.getAdresse() : null,
            image,
            c.getLead() != null ? c.getLead().getId() : null,
            visiteId,
            c.getDateDebut(),
            c.getDateFin(),
            c.getMontant(),
            c.getStatut(),
            c.getTypeContrat(),
            c.getDureeLocationMois(),
            c.getDocumentUrl(),
            c.getNotes(),
            c.getMotifResiliation(),
            c.getMotifProlongation(),
            c.getValideParClientAt(),
            c.getValideParSuperAdminAt(),
            c.getCreatedAt(),
            c.getUpdatedAt()
        );
    }
}
