package sn.immosn.backend.client.web.visite.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Affectation d'un administrateur responsable à une visite acceptée (Sprint 2).
 */
public record AffecterAdminDto(

    @NotNull(message = "L'identifiant de l'administrateur est obligatoire")
    Long adminId,

    String commentaire
) {}
