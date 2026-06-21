package sn.immosn.backend.client.web.proprietaire.dto;

// Résumé embarqué dans les DTOs Annonce — évite de charger les stats à chaque liste d'annonces
public record ProprietaireSummaryDto(
    Long id,
    String nomComplet,
    String telephone,
    String email
) {
}
