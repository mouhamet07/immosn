package sn.immosn.backend.client.web.dashboard.dto;

import java.time.LocalDateTime;

public record RecentActivityDto(
    String type,        // ANNONCE | VISITE | CONTRAT | SIGNALEMENT | CLIENT
    String titre,
    String description,
    String statut,
    LocalDateTime createdAt
) {}
