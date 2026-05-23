package sn.immosn.backend.client.web.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequestDto {

    @Size(min = 2, max = 100, message = "Le nom complet doit contenir entre 2 et 100 caractères")
    private String nomComplet;

    private String telephone;

    private String adresse;

    private String photo;

    @Email(message = "L'email doit être valide")
    private String email;

    private String motDePasseActuel;

    @Size(min = 8, message = "Le nouveau mot de passe doit avoir au moins 8 caractères")
    private String nouveauMotDePasse;
}
