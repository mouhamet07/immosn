package sn.immosn.backend.client.web.visite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Saisie du rapport de visite par l'administrateur responsable (Sprint 2).
 * Le document éventuel est téléversé séparément via l'endpoint dédié.
 */
public record RapportVisiteCreateRequestDto(

    @NotBlank(message = "Le compte-rendu est obligatoire")
    @Size(max = 10000, message = "Le compte-rendu ne peut pas dépasser 10000 caractères")
    String compteRendu,

    @NotNull(message = "L'issue de la visite (aboutie) est obligatoire")
    Boolean aboutie
) {}
