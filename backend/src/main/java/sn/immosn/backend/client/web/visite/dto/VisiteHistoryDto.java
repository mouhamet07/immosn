package sn.immosn.backend.client.web.visite.dto;

import java.time.LocalDateTime;

public record VisiteHistoryDto(
    Long id,
    String ancienStatut,
    String nouveauStatut,
    Long auteurId,
    String auteurEmail,
    LocalDateTime ancienneDateVisite,
    LocalDateTime nouvelleDateVisite,
    String action,
    String commentaire,
    LocalDateTime createdAt
) {}
