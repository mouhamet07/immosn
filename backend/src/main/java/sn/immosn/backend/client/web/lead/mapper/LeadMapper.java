package sn.immosn.backend.client.web.lead.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sn.immosn.backend.client.web.lead.dto.LeadResponseDto;
import sn.immosn.backend.contrat.data.repository.ContratRepository;
import sn.immosn.backend.lead.data.entity.Lead;

@Component
@RequiredArgsConstructor
public class LeadMapper {

    private final ContratRepository contratRepository;

    /** Utilisé pour les opérations sur un seul lead (getById, create, update). */
    public LeadResponseDto toDto(Lead l) {
        Long contratId = contratRepository.findFirstByLeadId(l.getId()).map(c -> c.getId()).orElse(null);
        return toDto(l, contratId);
    }

    /**
     * Utilisé pour les listes paginées : contratId pré-chargé en batch par le service
     * pour éviter le N+1 (1 requête batch au lieu de 1 requête par lead).
     */
    public LeadResponseDto toDto(Lead l, Long contratId) {
        String image = (l.getAnnonce().getImages() != null && !l.getAnnonce().getImages().isEmpty())
            ? l.getAnnonce().getImages().get(0) : null;
        var prospect = l.getProspect();
        String prospectNom = prospect != null
            ? ((prospect.getPrenom() != null && !prospect.getPrenom().isBlank() ? prospect.getPrenom().trim() + " " : "")
                + (prospect.getNom() != null ? prospect.getNom() : "")).trim()
            : null;
        return new LeadResponseDto(
            l.getId(),
            l.getClient() != null ? l.getClient().getId() : null,
            l.getClient() != null ? l.getClient().getNomComplet() : null,
            l.getClient() != null ? l.getClient().getEmail() : null,
            prospect != null ? prospect.getId() : null,
            prospectNom,
            l.getAnnonce().getId(),
            l.getAnnonce().getLibelle(),
            l.getAnnonce().getAdresse(),
            image,
            l.getAnnonce().getTypeTransaction(),
            l.getVisite() != null ? l.getVisite().getId() : null,
            contratId,
            l.getStatut(),
            l.getNoteAdmin(),
            l.getCreatedAt(),
            l.getUpdatedAt(),
            l.getConvertedAt()
        );
    }
}
