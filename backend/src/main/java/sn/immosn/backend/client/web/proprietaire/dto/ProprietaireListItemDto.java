package sn.immosn.backend.client.web.proprietaire.dto;

import java.time.LocalDateTime;

// Item de liste admin — inclut un résumé des statistiques pour affichage direct dans le tableau
public record ProprietaireListItemDto(
    Long id,
    String nomComplet,
    String email,
    String telephone,
    String adresse,
    Boolean isArchived,
    LocalDateTime createdAt,
    ProprietaireStatsDto statistiques
) {
}
