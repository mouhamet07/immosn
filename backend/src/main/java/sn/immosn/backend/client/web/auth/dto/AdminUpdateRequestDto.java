package sn.immosn.backend.client.web.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Requête de modification d'un administrateur par le SUPER_ADMIN.
 *
 * Tous les champs sont optionnels : seuls ceux fournis (non nuls / non vides)
 * sont mis à jour. Le mot de passe n'est réinitialisé que si {@code nouveauMotDePasse}
 * est fourni, et reste soumis aux mêmes règles de complexité que l'inscription.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateRequestDto {

    @Size(min = 2, max = 100, message = "Le nom complet doit contenir entre 2 et 100 caractères")
    private String nomComplet;

    @Email(message = "L'email doit être valide")
    private String email;

    private String telephone;

    @Size(min = 8, max = 128, message = "Le mot de passe doit contenir entre 8 et 128 caractères")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).+$",
        message = "Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial"
    )
    private String nouveauMotDePasse;
}
