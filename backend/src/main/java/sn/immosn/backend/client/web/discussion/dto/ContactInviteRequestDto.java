package sn.immosn.backend.client.web.discussion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Premier contact d'un visiteur non authentifié avec l'agence à propos d'une annonce.
 * Crée un prospect (ou le réutilise par email) et ouvre une discussion avec un premier message.
 */
public record ContactInviteRequestDto(

    @NotNull(message = "L'identifiant de l'annonce est obligatoire")
    Long annonceId,

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 120, message = "Le nom ne peut pas dépasser 120 caractères")
    String nom,

    @Size(max = 120, message = "Le prénom ne peut pas dépasser 120 caractères")
    String prenom,

    @NotBlank(message = "Le téléphone est obligatoire")
    @Size(max = 30, message = "Le téléphone ne peut pas dépasser 30 caractères")
    String telephone,

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Le format de l'email est invalide")
    @Size(max = 180, message = "L'email ne peut pas dépasser 180 caractères")
    String email,

    @Size(max = 255, message = "L'adresse ne peut pas dépasser 255 caractères")
    String adresse,

    @NotBlank(message = "Le premier message ne peut pas être vide")
    @Size(max = 4000, message = "Le message ne peut pas dépasser 4000 caractères")
    String premierMessage
) {}
