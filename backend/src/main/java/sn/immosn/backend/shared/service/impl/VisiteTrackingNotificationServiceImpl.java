package sn.immosn.backend.shared.service.impl;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import sn.immosn.backend.shared.service.VisiteTrackingNotificationService;

/**
 * Implémentation Twilio (SMS) + JavaMailSender (email) de l'envoi du numéro de suivi.
 * Chaque canal est activé indépendamment via twilio.enabled / mail.enabled — si désactivé ou
 * en erreur, l'envoi est journalisé sans bloquer la création de la demande de visite.
 */
@Slf4j
@Service
public class VisiteTrackingNotificationServiceImpl implements VisiteTrackingNotificationService {

    private final JavaMailSender mailSender;

    @Value("${twilio.enabled:false}")
    private boolean twilioEnabled;
    @Value("${twilio.account-sid:}")
    private String twilioAccountSid;
    @Value("${twilio.auth-token:}")
    private String twilioAuthToken;
    @Value("${twilio.from-number:}")
    private String twilioFromNumber;

    @Value("${mail.enabled:false}")
    private boolean mailEnabled;
    @Value("${mail.from:noreply@immosn.sn}")
    private String mailFrom;

    public VisiteTrackingNotificationServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    void initTwilio() {
        if (twilioEnabled) {
            Twilio.init(twilioAccountSid, twilioAuthToken);
        }
    }

    @Override
    public void notifierNumeroSuivi(String nom, String email, String telephone, String token) {
        envoyerSms(telephone, token);
        envoyerEmail(nom, email, token);
    }

    private void envoyerSms(String telephone, String token) {
        if (!twilioEnabled) {
            log.info("[SMS] Twilio désactivé — numéro de suivi non envoyé par SMS (telephone={})", maskTelephone(telephone));
            return;
        }
        try {
            log.info("[SMS] Tentative d'envoi du numéro de suivi par SMS (telephone={})", maskTelephone(telephone));
            Message.creator(
                new PhoneNumber(telephone),
                new PhoneNumber(twilioFromNumber),
                "ImmoSN — votre numéro de suivi est : " + token
            ).create();
            log.info("[SMS] Numéro de suivi envoyé avec succès (telephone={})", maskTelephone(telephone));
        } catch (Exception e) {
            log.error("[SMS] Échec d'envoi du numéro de suivi à {} : {}", maskTelephone(telephone), e.getMessage());
        }
    }

    private void envoyerEmail(String nom, String email, String token) {
        if (!mailEnabled) {
            log.info("[EMAIL] Envoi désactivé — numéro de suivi non envoyé par mail (email={})", email);
            return;
        }
        try {
            log.info("[EMAIL] Tentative d'envoi du numéro de suivi par email (destinataire={})", email);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("ImmoSN — Votre numéro de suivi de visite");
            message.setText("Bonjour " + nom + ",\n\n"
                + "Votre demande de visite a bien été enregistrée.\n"
                + "Votre numéro de suivi est : " + token + "\n\n"
                + "Conservez-le pour suivre l'avancement de votre demande.\n\n"
                + "L'équipe ImmoSN");
            mailSender.send(message);
            log.info("[EMAIL] Numéro de suivi envoyé avec succès (destinataire={})", email);
        } catch (Exception e) {
            log.error("[EMAIL] Échec d'envoi du numéro de suivi à {} : {}", email, e.getMessage());
        }
    }

    private String maskTelephone(String telephone) {
        if (telephone == null || telephone.length() < 4) return "****";
        return "*".repeat(telephone.length() - 4) + telephone.substring(telephone.length() - 4);
    }
}
