package sn.immosn.backend.shared.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import sn.immosn.backend.shared.service.impl.VisiteTrackingNotificationServiceImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Vérifie que l'envoi du numéro de suivi (email + SMS) au prospect après une demande de visite
 * invité ne bloque jamais la création de la demande, même si le provider mail est indisponible.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VisiteTrackingNotificationServiceImpl — numéro de suivi visite invité")
class VisiteTrackingNotificationServiceTest {

    @Mock JavaMailSender mailSender;

    private VisiteTrackingNotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VisiteTrackingNotificationServiceImpl(mailSender);
        ReflectionTestUtils.setField(service, "mailEnabled", true);
        ReflectionTestUtils.setField(service, "mailFrom", "noreply@immosn.sn");
        ReflectionTestUtils.setField(service, "twilioEnabled", false);
    }

    @Test
    @DisplayName("notifierNumeroSuivi — envoie l'email quand mail.enabled=true")
    void notifie_sendsEmail() {
        service.notifierNumeroSuivi("Diallo", "aminata@email.com", "+221770000001", "tok-123");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("notifierNumeroSuivi — mail.enabled=false : aucun envoi, aucune exception")
    void notifie_mailDisabled_noSend() {
        ReflectionTestUtils.setField(service, "mailEnabled", false);

        service.notifierNumeroSuivi("Diallo", "aminata@email.com", "+221770000001", "tok-123");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("notifierNumeroSuivi — échec SMTP : ne lève aucune exception (création de visite jamais bloquée)")
    void notifie_smtpFailure_doesNotThrow() {
        doThrow(new MailSendException("SMTP indisponible")).when(mailSender).send(any(SimpleMailMessage.class));

        service.notifierNumeroSuivi("Diallo", "aminata@email.com", "+221770000001", "tok-123");
        // Aucune assertion d'exception : le test échouerait si l'exception remontait.
    }
}
