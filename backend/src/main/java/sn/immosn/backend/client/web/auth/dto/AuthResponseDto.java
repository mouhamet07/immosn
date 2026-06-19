package sn.immosn.backend.client.web.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private Long id;
    private String nomComplet;
    private String email;
    private String telephone;
    private String adresse;
    private String photo;
    private LocalDateTime creationDate;
    private boolean archived;
    private String accessToken;
    private String tokenType;
    private Set<String> roles;
    private LocalDateTime dernierConnexion;
    private long sessionsActives;
    /** Sprint 3 : true si l'utilisateur doit changer son mot de passe temporaire. */
    private boolean motDePasseAChanger;
}
