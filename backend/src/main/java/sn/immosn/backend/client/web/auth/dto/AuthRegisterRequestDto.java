package sn.immosn.backend.client.web.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthRegisterRequestDto {
    @NotBlank(message = "Le nom complet est requis")
    private String nomComplet;

    @NotBlank(message = "L'email est requis")
    @Email(message = "L'email doit être valide")
    private String email;

    @NotBlank(message = "Le téléphone est requis")
    private String telephone;

    @NotBlank(message = "Le mot de passe est requis")
    private String motDePasse;
}
