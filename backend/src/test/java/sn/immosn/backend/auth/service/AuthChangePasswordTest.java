package sn.immosn.backend.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.jwt.JwtTokenProvider;
import sn.immosn.backend.auth.data.repository.BlacklistedTokenRepository;
import sn.immosn.backend.auth.data.repository.RoleRepository;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.auth.data.repository.UserSessionRepository;
import sn.immosn.backend.client.web.auth.dto.ChangePasswordRequestDto;
import sn.immosn.backend.client.web.auth.mapper.AuthMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — changement de mot de passe obligatoire (Sprint 3)")
class AuthChangePasswordTest {

    @Mock AuthUserDetailService authUserDetailService;
    @Mock RoleRepository roleRepository;
    @Mock UserRepository userRepository;
    @Mock AuthenticationManager authenticationManager;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthMapper authMapper;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock BlacklistedTokenRepository blacklistedTokenRepository;
    @Mock UserSessionRepository userSessionRepository;

    @InjectMocks AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(42L);
        user.setEmail("aminata@email.com");
        user.setPassword("$old$");
    }

    private ChangePasswordRequestDto req(String actuel, String nouveau, String confirm) {
        ChangePasswordRequestDto dto = new ChangePasswordRequestDto();
        dto.setMotDePasseActuel(actuel);
        dto.setNouveauMotDePasse(nouveau);
        dto.setConfirmationMotDePasse(confirm);
        return dto;
    }

    @Test
    @DisplayName("compte généré (motDePasseAChanger=true) — change sans mot de passe actuel et lève l'indicateur")
    void changePassword_temporary_clearsFlag() {
        user.setMotDePasseAChanger(true);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NouveauPass@123")).thenReturn("$new$");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(authMapper.toAuthResponseDto(any(), any(), anyLong())).thenReturn(null);

        authService.changePassword(42L, req(null, "NouveauPass@123", "NouveauPass@123"));

        assertThat(user.getPassword()).isEqualTo("$new$");
        assertThat(user.isMotDePasseAChanger()).isFalse();
        // Pas de vérification du mot de passe actuel pour un compte à mot de passe temporaire
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("changement volontaire — confirmation différente → IllegalStateException")
    void changePassword_mismatch() {
        user.setMotDePasseAChanger(false);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Actuel@123", "$old$")).thenReturn(true);

        assertThatThrownBy(() ->
            authService.changePassword(42L, req("Actuel@123", "NouveauPass@123", "AutreChose@123")))
            .isInstanceOf(IllegalStateException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("changement volontaire — mot de passe actuel incorrect → IllegalStateException")
    void changePassword_wrongCurrent() {
        user.setMotDePasseAChanger(false);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Faux", "$old$")).thenReturn(false);

        assertThatThrownBy(() ->
            authService.changePassword(42L, req("Faux", "NouveauPass@123", "NouveauPass@123")))
            .isInstanceOf(IllegalStateException.class);
    }
}
