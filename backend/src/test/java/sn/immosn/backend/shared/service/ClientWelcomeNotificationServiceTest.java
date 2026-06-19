package sn.immosn.backend.shared.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import sn.immosn.backend.auth.data.entity.RoleType;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.shared.service.impl.ClientWelcomeNotificationServiceImpl;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Vérifie que l'envoi des identifiants (email + SMS) et la notification SUPER_ADMIN sont
 * réellement déclenchés, et qu'une panne SMTP/Twilio ne fait jamais échouer la conversion
 * prospect → client (aucune exception ne doit remonter à l'appelant).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientWelcomeNotificationServiceImpl — envoi identifiants + notification SUPER_ADMIN")
class ClientWelcomeNotificationServiceTest {

    @Mock JavaMailSender mailSender;
    @Mock UserRepository userRepository;

    private ClientWelcomeNotificationServiceImpl service;
    private User client;
    private User superAdmin;

    @BeforeEach
    void setUp() {
        service = new ClientWelcomeNotificationServiceImpl(mailSender, userRepository);
        ReflectionTestUtils.setField(service, "mailEnabled", true);
        ReflectionTestUtils.setField(service, "mailFrom", "noreply@immosn.sn");
        ReflectionTestUtils.setField(service, "twilioEnabled", false);

        client = new User();
        client.setId(42L);
        client.setNomComplet("Aminata Diallo");
        client.setEmail("aminata@email.com");
        client.setTelephone("+221770000001");
        client.setCreationDate(LocalDateTime.now());

        superAdmin = new User();
        superAdmin.setId(1L);
        superAdmin.setEmail("superadmin@immosn.sn");
    }

    @Test
    @DisplayName("sendCredentials — envoie l'email de bienvenue au client quand mail.enabled=true")
    void sendCredentials_sendsWelcomeEmail() {
        when(userRepository.findByRoles_Role(eq(RoleType.SUPER_ADMIN), any(Pageable.class)))
            .thenReturn(Page.empty());

        service.sendCredentials(client, "Tmp@1234");

        verify(mailSender, atLeastOnce()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendCredentials — notifie chaque SUPER_ADMIN trouvé")
    void sendCredentials_notifiesSuperAdmins() {
        when(userRepository.findByRoles_Role(eq(RoleType.SUPER_ADMIN), any(Pageable.class)))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(superAdmin)));

        service.sendCredentials(client, "Tmp@1234");

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class)); // 1 client + 1 super admin
    }

    @Test
    @DisplayName("sendCredentials — mail.enabled=false : aucun envoi, aucune exception")
    void sendCredentials_mailDisabled_noSend() {
        ReflectionTestUtils.setField(service, "mailEnabled", false);

        service.sendCredentials(client, "Tmp@1234");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("sendCredentials — échec SMTP : ne lève aucune exception (non bloquant)")
    void sendCredentials_smtpFailure_doesNotThrow() {
        when(userRepository.findByRoles_Role(eq(RoleType.SUPER_ADMIN), any(Pageable.class)))
            .thenReturn(Page.empty());
        doThrow(new MailSendException("SMTP indisponible")).when(mailSender).send(any(SimpleMailMessage.class));

        service.sendCredentials(client, "Tmp@1234");
        // Aucune assertion d'exception : le test échouerait si sendCredentials() la propageait.
    }

    @Test
    @DisplayName("sendCredentials — aucun SUPER_ADMIN trouvé : pas d'erreur, email client tout de même envoyé")
    void sendCredentials_noSuperAdminFound_doesNotThrow() {
        when(userRepository.findByRoles_Role(eq(RoleType.SUPER_ADMIN), any(Pageable.class)))
            .thenReturn(Page.empty());

        service.sendCredentials(client, "Tmp@1234");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
