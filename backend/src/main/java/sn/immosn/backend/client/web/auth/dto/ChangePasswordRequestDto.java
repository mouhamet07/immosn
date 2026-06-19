package sn.immosn.backend.client.web.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Changement de mot de passe (Sprint 3).
 *
 * <p>{@code motDePasseActuel} est requis pour un changement volontaire, mais facultatif lorsque
 * l'utilisateur a un mot de passe temporaire à changer (compte généré automatiquement).</p>
 */
@Getter
@Setter
public class ChangePasswordRequestDto {

    private String motDePasseActuel;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 8, message = "Le nouveau mot de passe doit contenir au moins 8 caractères")
    private String nouveauMotDePasse;

    @NotBlank(message = "La confirmation du mot de passe est obligatoire")
    private String confirmationMotDePasse;
}
