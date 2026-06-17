package sn.immosn.backend.client.web.visite.mapper;

import org.springframework.stereotype.Component;
import sn.immosn.backend.client.web.visite.dto.DemandeVisiteResponseDto;
import sn.immosn.backend.client.web.visite.dto.VisiteInviteResponseDto;
import sn.immosn.backend.visite.data.entity.DemandeVisite;

@Component
public class DemandeVisiteMapper {

    public DemandeVisiteResponseDto toDto(DemandeVisite v) {
        String image = (v.getAnnonce().getImages() != null && !v.getAnnonce().getImages().isEmpty())
            ? v.getAnnonce().getImages().get(0) : null;
        // Le client est nullable depuis l'ouverture du parcours visiteur : pour une demande invité,
        // l'identité provient des champs invité (et du prénom). On expose alors clientId=null.
        Long clientId = v.getClient() != null ? v.getClient().getId() : null;
        return new DemandeVisiteResponseDto(
            v.getId(),
            clientId,
            resolveNom(v),
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

    /** Réponse dédiée au flux invité : inclut le token de suivi du prospect. */
    public VisiteInviteResponseDto toInviteDto(DemandeVisite v, String prospectToken) {
        return new VisiteInviteResponseDto(
            v.getId(),
            prospectToken,
            v.getNom(),
            v.getPrenom(),
            v.getEmail(),
            v.getTelephone(),
            v.getAnnonce().getId(),
            v.getAnnonce().getLibelle(),
            v.getAnnonce().getAdresse(),
            v.getDateVisite(),
            v.getHeureVisite(),
            v.getStatut(),
            v.getCommentaire(),
            v.getCreatedAt()
        );
    }

    private String resolveNom(DemandeVisite v) {
        if (v.getClient() != null) {
            return v.getClient().getNomComplet();
        }
        String prenom = v.getPrenom() != null ? v.getPrenom() + " " : "";
        return (prenom + (v.getNom() != null ? v.getNom() : "")).trim();
    }
}
