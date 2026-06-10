package sn.immosn.backend.client.web.lead.dto;

import java.time.LocalDateTime;

public record LeadHistoryDto(
    Long id,
    String ancienStatut,
    String nouveauStatut,
    Long auteurId,
    String auteurEmail,
    String action,
    String commentaire,
    LocalDateTime createdAt
) {}
