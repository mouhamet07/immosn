package sn.immosn.backend.client.web.contrat.mapper;

import org.springframework.stereotype.Component;
import sn.immosn.backend.client.web.contrat.dto.ContratDocumentDto;
import sn.immosn.backend.client.web.contrat.dto.ContratResponseDto;
import sn.immosn.backend.contrat.data.entity.Contrat;
import sn.immosn.backend.contrat.data.entity.ContratDocument;

import java.util.Collections;
import java.util.List;

@Component
public class ContratMapper {

    public ContratResponseDto toDto(Contrat c) {
        return toDto(c, Collections.emptyList());
    }

    public ContratResponseDto toDto(Contrat c, List<ContratDocument> documents) {
        var annonce = c.getAnnonce();
        String image = (annonce != null && annonce.getImages() != null && !annonce.getImages().isEmpty())
            ? annonce.getImages().get(0) : null;

        Long visiteId = (c.getLead() != null && c.getLead().getVisite() != null)
            ? c.getLead().getVisite().getId() : null;

        var prospect = c.getProspect();
        String prospectNom = prospect != null
            ? ((prospect.getPrenom() != null && !prospect.getPrenom().isBlank() ? prospect.getPrenom().trim() + " " : "")
                + (prospect.getNom() != null ? prospect.getNom() : "")).trim()
            : null;

        return new ContratResponseDto(
            c.getId(),
            c.getClient() != null ? c.getClient().getId() : null,
            c.getClient() != null ? c.getClient().getNomComplet() : null,
            prospect != null ? prospect.getId() : null,
            prospectNom,
            prospect != null ? prospect.getEmail() : null,
            prospect != null ? prospect.getTelephone() : null,
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
            c.getValideParSuperAdminAt(),
            c.getCreatedAt(),
            c.getUpdatedAt(),
            documents.stream().map(this::toDocumentDto).toList()
        );
    }

    public ContratDocumentDto toDocumentDto(ContratDocument d) {
        return new ContratDocumentDto(
            d.getId(),
            d.getType(),
            d.getUrl(),
            d.getUploadedBy() != null ? d.getUploadedBy().getNomComplet() : null,
            d.getCreatedAt()
        );
    }
}
