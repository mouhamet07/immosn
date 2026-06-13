package sn.immosn.backend.client.web.contrat.dto;

import java.time.LocalDateTime;

public record ContratHistoryDto(
    Long id,
    String ancienStatut,
    String nouveauStatut,
    Long auteurId,
    String auteurEmail,
    String action,
    String commentaire,
    LocalDateTime createdAt
) {}
