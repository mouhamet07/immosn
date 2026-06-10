package sn.immosn.backend.auth.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import sn.immosn.backend.auth.service.AuthService;
import sn.immosn.backend.client.web.auth.controller.AuthController;
import sn.immosn.backend.client.web.auth.dto.AuthLoginRequestDto;
import sn.immosn.backend.client.web.auth.dto.AuthRegisterRequestDto;
import sn.immosn.backend.client.web.auth.dto.AuthResponseDto;
import sn.immosn.backend.client.web.auth.mapper.AuthMapper;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du contrôleur Auth.
 * AuthResponseDto est un POJO Lombok → utiliser les getters (getEmail(), getAccessToken()…).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController — Tests unitaires")
class AuthControllerTest {

    @Mock AuthService authService;
    @Mock AuthMapper  authMapper;
    @InjectMocks AuthController authController;

    private AuthResponseDto buildResponse(String email) {
        // AuthResponseDto est un POJO avec @AllArgsConstructor Lombok
        // Signature : id, nomComplet, email, telephone, adresse, photo, creationDate, archived, accessToken, tokenType, roles
        return new AuthResponseDto(
            1L, "Test User", email, "+221770000001",
            null, null,
            LocalDateTime.now(), false, "jwt-token-xxx", "Bearer", Set.of("CLIENT")
        );
    }

    // register — retourne CREATED

    @Test
    @DisplayName("register — délègue à AuthService et retourne 201 CREATED")
    void register_delegates() {
        var req = new AuthRegisterRequestDto(
            "Test User", "user@immosn.sn", "+221770000001", "password123"
        );
        when(authService.register(req)).thenReturn(buildResponse("user@immosn.sn"));

        var response = authController.register(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        // AuthResponseDto est un POJO → getEmail() (pas email())
        assertThat(response.getBody().data().getEmail()).isEqualTo("user@immosn.sn");
        verify(authService).register(req);
    }

    // login — retourne OK

    @Test
    @DisplayName("login — délègue à AuthService et retourne le token")
    void login_delegates() {
        var req = new AuthLoginRequestDto("user@immosn.sn", "password123");
        when(authService.login(req)).thenReturn(buildResponse("user@immosn.sn"));

        var response = authController.login(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // AuthResponseDto est un POJO → getAccessToken() (pas accessToken())
        assertThat(response.getBody().data().getAccessToken()).isEqualTo("jwt-token-xxx");
        verify(authService).login(req);
    }

    // logout — header valide → 204

    @Test
    @DisplayName("logout — Bearer valide → délègue et retourne 204")
    void logout_validBearer() {
        doNothing().when(authService).logout("jwt-token-xxx");

        var response = authController.logout("Bearer jwt-token-xxx");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(authService).logout("jwt-token-xxx");
    }

    // logout — header manquant → 400

    @Test
    @DisplayName("logout — header absent → 400 BAD_REQUEST")
    void logout_missingHeader() {
        var response = authController.logout(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(authService);
    }
}
