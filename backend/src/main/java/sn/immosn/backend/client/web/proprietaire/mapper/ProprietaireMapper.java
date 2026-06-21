package sn.immosn.backend.client.web.proprietaire.mapper;

import org.springframework.stereotype.Component;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireCreateRequestDto;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireListItemDto;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireResponseDto;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireStatsDto;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireSummaryDto;
import sn.immosn.backend.proprietaire.data.entity.Proprietaire;

@Component
public class ProprietaireMapper {

    public ProprietaireResponseDto toResponse(Proprietaire p) {
        return new ProprietaireResponseDto(
            p.getId(),
            p.getNomComplet(),
            p.getTelephone(),
            p.getEmail(),
            p.getAdresse(),
            p.getNotes(),
            p.isArchived(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }

    public ProprietaireListItemDto toListItem(Proprietaire p, ProprietaireStatsDto stats) {
        return new ProprietaireListItemDto(
            p.getId(),
            p.getNomComplet(),
            p.getEmail(),
            p.getTelephone(),
            p.getAdresse(),
            p.isArchived(),
            p.getCreatedAt(),
            stats
        );
    }

    public ProprietaireSummaryDto toSummary(Proprietaire p) {
        if (p == null) return null;
        return new ProprietaireSummaryDto(p.getId(), p.getNomComplet(), p.getTelephone(), p.getEmail());
    }

    public Proprietaire toEntity(ProprietaireCreateRequestDto dto) {
        return Proprietaire.builder()
            .nomComplet(dto.nomComplet())
            .telephone(dto.telephone())
            .email(dto.email())
            .adresse(dto.adresse())
            .notes(dto.notes())
            .build();
    }
}
