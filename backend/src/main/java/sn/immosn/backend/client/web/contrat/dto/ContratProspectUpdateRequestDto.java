package sn.immosn.backend.client.web.contrat.dto;

import jakarta.validation.constraints.NotBlank;

public record ContratProspectUpdateRequestDto(
    @NotBlank(message = "Le nom est obligatoire") String nom,
    String prenom,
    @NotBlank(message = "L'email est obligatoire") String email,
    @NotBlank(message = "Le téléphone est obligatoire") String telephone
) {}
