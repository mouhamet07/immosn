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

    public LeadResponseDto toDto(Lead l) {
        String image = (l.getAnnonce().getImages() != null && !l.getAnnonce().getImages().isEmpty())
            ? l.getAnnonce().getImages().get(0) : null;
        Long contratId = contratRepository.findFirstByLeadId(l.getId())
            .map(c -> c.getId()).orElse(null);
        return new LeadResponseDto(
            l.getId(),
            l.getClient().getId(),
            l.getClient().getNomComplet(),
            l.getClient().getEmail(),
            l.getAnnonce().getId(),
            l.getAnnonce().getLibelle(),
            l.getAnnonce().getAdresse(),
            image,
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
