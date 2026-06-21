package sn.immosn.backend.client.web.proprietaire.dto;

public record ProprietaireDetailDto(
    ProprietaireResponseDto proprietaire,
    ProprietaireStatsDto stats
) {
}
