package sn.immosn.backend.prospect.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import sn.immosn.backend.auth.data.entity.Role;
import sn.immosn.backend.auth.data.entity.RoleType;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.repository.RoleRepository;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.prospect.dto.ConversionResultDto;
import sn.immosn.backend.discussion.data.entity.Discussion;
import sn.immosn.backend.discussion.data.repository.DiscussionRepository;
import sn.immosn.backend.prospect.data.entity.Prospect;
import sn.immosn.backend.prospect.data.repository.ProspectRepository;
import sn.immosn.backend.prospect.service.impl.ProspectConversionServiceImpl;
import sn.immosn.backend.shared.exception.EntityExistException;
import sn.immosn.backend.shared.service.NotificationService;
import sn.immosn.backend.visite.data.entity.DemandeVisite;
import sn.immosn.backend.visite.data.repository.DemandeVisiteRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProspectConversionService — conversion prospect → client (Sprint 3)")
class ProspectConversionServiceTest {

    @Mock ProspectRepository prospectRepository;
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock DiscussionRepository discussionRepository;
    @Mock DemandeVisiteRepository visiteRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock NotificationService notificationService;

    @InjectMocks ProspectConversionServiceImpl service;

    private Prospect prospect;

    @BeforeEach
    void setUp() {
        prospect = Prospect.builder()
            .nom("Diallo").prenom("Aminata")
            .email("aminata@email.com").telephone("+221770000001")
            .adresse("Almadies, Dakar").token("ptok")
            .build();
        prospect.setId(7L);
    }

    @Test
    @DisplayName("convertirProspect — crée un User CLIENT avec compte généré + mdp à changer, encodé BCrypt")
    void convertir_createsClientAccount() {
        when(prospectRepository.findById(7L)).thenReturn(Optional.of(prospect));
        when(userRepository.existsByEmail("aminata@email.com")).thenReturn(false);
        when(roleRepository.findByRole(RoleType.CLIENT)).thenReturn(Optional.of(new Role(1L, RoleType.CLIENT)));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$encoded$");
        when(discussionRepository.findByProspectId(7L)).thenReturn(List.of());
        when(visiteRepository.findByProspectId(7L)).thenReturn(List.of());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });

        ConversionResultDto dto = service.convertirProspect(7L);

        assertThat(dto.userId()).isEqualTo(42L);
        assertThat(dto.email()).isEqualTo("aminata@email.com");
        assertThat(dto.compteGenereAuto()).isTrue();
        assertThat(dto.motDePasseAChanger()).isTrue();
        assertThat(dto.dejaConverti()).isFalse();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User created = userCaptor.getValue();
        assertThat(created.getNomComplet()).isEqualTo("Aminata Diallo");
        assertThat(created.getPassword()).isEqualTo("$2a$encoded$");
        assertThat(created.isCompteGenereAuto()).isTrue();
        assertThat(created.isMotDePasseAChanger()).isTrue();
        assertThat(created.getRoles()).anyMatch(r -> r.getRole() == RoleType.CLIENT);

        // 2 : un mot de passe temporaire est généré puis encodé
        ArgumentCaptor<String> pwd = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(pwd.capture());
        assertThat(pwd.getValue()).isNotBlank().hasSizeGreaterThanOrEqualTo(12);

        // Le prospect est marqué converti et les identifiants sont transmis
        assertThat(prospect.getConvertedUser()).isNotNull();
        verify(notificationService).sendCredentials(any(User.class), anyString());
    }

    @Test
    @DisplayName("convertirProspect — rattache discussions et visites du prospect au nouveau compte")
    void convertir_reattachesData() {
        Discussion d = Discussion.builder().prospect(prospect).guestToken("g").build();
        DemandeVisite v = DemandeVisite.builder().prospect(prospect).build();

        when(prospectRepository.findById(7L)).thenReturn(Optional.of(prospect));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByRole(RoleType.CLIENT)).thenReturn(Optional.of(new Role(1L, RoleType.CLIENT)));
        when(passwordEncoder.encode(anyString())).thenReturn("enc");
        when(discussionRepository.findByProspectId(7L)).thenReturn(List.of(d));
        when(visiteRepository.findByProspectId(7L)).thenReturn(List.of(v));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> { User u = inv.getArgument(0); u.setId(42L); return u; });

        ConversionResultDto dto = service.convertirProspect(7L);

        assertThat(dto.discussionsRattachees()).isEqualTo(1);
        assertThat(dto.visitesRattachees()).isEqualTo(1);
        assertThat(d.getClient()).isNotNull();
        assertThat(v.getClient()).isNotNull();
        verify(discussionRepository).saveAll(any());
        verify(visiteRepository).saveAll(any());
    }

    @Test
    @DisplayName("convertirProspect — idempotent : prospect déjà converti renvoie le compte existant")
    void convertir_idempotent() {
        User existing = new User();
        existing.setId(99L);
        existing.setEmail("aminata@email.com");
        existing.setNomComplet("Aminata Diallo");
        existing.setCompteGenereAuto(true);
        prospect.setConvertedUser(existing);
        when(prospectRepository.findById(7L)).thenReturn(Optional.of(prospect));

        ConversionResultDto dto = service.convertirProspect(7L);

        assertThat(dto.dejaConverti()).isTrue();
        assertThat(dto.userId()).isEqualTo(99L);
        verify(userRepository, never()).save(any());
        verify(notificationService, never()).sendCredentials(any(), anyString());
    }

    @Test
    @DisplayName("convertirProspect — email déjà utilisé par un compte → EntityExistException")
    void convertir_emailCollision() {
        when(prospectRepository.findById(7L)).thenReturn(Optional.of(prospect));
        when(userRepository.existsByEmail("aminata@email.com")).thenReturn(true);

        assertThatThrownBy(() -> service.convertirProspect(7L))
            .isInstanceOf(EntityExistException.class);
        verify(userRepository, never()).save(any());
    }
}
