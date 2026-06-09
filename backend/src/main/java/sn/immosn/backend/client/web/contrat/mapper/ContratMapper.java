package sn.immosn.backend.client.web.contrat.mapper;

import org.springframework.stereotype.Component;
import sn.immosn.backend.client.web.contrat.dto.ContratResponseDto;
import sn.immosn.backend.contrat.data.entity.Contrat;

@Component
public class ContratMapper {

    public ContratResponseDto toDto(Contrat c) {
        String image = (c.getAnnonce().getImages() != null && !c.getAnnonce().getImages().isEmpty())
            ? c.getAnnonce().getImages().get(0) : null;

        Long visiteId = (c.getLead() != null && c.getLead().getVisite() != null)
            ? c.getLead().getVisite().getId() : null;

        return new ContratResponseDto(
            c.getId(),
            c.getClient().getId(),
            c.getClient().getNomComplet(),
            c.getAnnonce().getId(),
            c.getAnnonce().getLibelle(),
            c.getAnnonce().getAdresse(),
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
            c.getCreatedAt(),
            c.getUpdatedAt()
        );
    }
}
