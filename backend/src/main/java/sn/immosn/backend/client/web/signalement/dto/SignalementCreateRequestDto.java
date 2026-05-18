package sn.immosn.backend.client.web.signalement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SignalementCreateRequestDto(
    @NotNull(message = "L'identifiant du contrat est obligatoire")
    Long contratId,

    @NotBlank(message = "Le contenu du signalement est obligatoire")
    String contenu
) {}
