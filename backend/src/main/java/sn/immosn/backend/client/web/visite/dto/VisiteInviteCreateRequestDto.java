package sn.immosn.backend.client.web.visite.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Demande de visite déposée par un visiteur non authentifié.
 * Reprend les champs du formulaire invité : identité complète + créneau souhaité.
 */
public record VisiteInviteCreateRequestDto(

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

    @NotNull(message = "La date de visite souhaitée est obligatoire")
    @Future(message = "La date de visite doit être dans le futur")
    LocalDateTime dateVisite,

    @Size(max = 10, message = "L'heure souhaitée est invalide")
    String heureVisite,

    @Size(max = 2000, message = "Le commentaire ne peut pas dépasser 2000 caractères")
    String commentaire
) {}
