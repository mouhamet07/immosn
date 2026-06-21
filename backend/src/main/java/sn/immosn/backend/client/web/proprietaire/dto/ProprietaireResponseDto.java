package sn.immosn.backend.client.web.proprietaire.dto;

import java.time.LocalDateTime;

public record ProprietaireResponseDto(
    Long id,
    String nomComplet,
    String telephone,
    String email,
    String adresse,
    String notes,
    Boolean isArchived,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
