package sn.immosn.backend.client.web.proprietaire.dto;

import java.math.BigDecimal;

public record ProprietaireStatsDto(
    long totalBiens,
    long biensVente,
    long biensLocation,
    long annoncesActives,
    long visites,
    long contrats,
    BigDecimal revenus
) {
}
