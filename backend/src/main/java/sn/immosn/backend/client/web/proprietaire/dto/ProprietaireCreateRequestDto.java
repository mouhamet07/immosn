package sn.immosn.backend.client.web.proprietaire.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProprietaireCreateRequestDto(

    @NotBlank(message = "Le nom complet est obligatoire")
    @Size(min = 2, max = 150, message = "Le nom complet doit contenir entre 2 et 150 caractères")
    String nomComplet,

    @NotBlank(message = "Le téléphone est obligatoire")
    String telephone,

    @Email(message = "L'email doit être valide")
    String email,

    @Size(max = 500, message = "L'adresse ne peut pas dépasser 500 caractères")
    String adresse,

    @Size(max = 2000, message = "Les notes ne peuvent pas dépasser 2000 caractères")
    String notes
) {}
