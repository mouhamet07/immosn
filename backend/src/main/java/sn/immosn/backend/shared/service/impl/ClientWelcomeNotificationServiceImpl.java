package sn.immosn.backend.shared.service.impl;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import sn.immosn.backend.auth.data.entity.RoleType;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.shared.service.NotificationService;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Envoi réel des identifiants de connexion (email + SMS) au client converti, et notification
 * email du/des SUPER_ADMIN à chaque création de compte. Chaque canal est indépendant : l'échec
 * de l'un n'empêche jamais la conversion prospect → client (try/catch, jamais de rethrow).
 */
@Slf4j
@Service
public class ClientWelcomeNotificationServiceImpl implements NotificationService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${twilio.enabled:false}")
    private boolean twilioEnabled;
    @Value("${twilio.from-number:}")
    private String twilioFromNumber;

    @Value("${mail.enabled:false}")
    private boolean mailEnabled;
    @Value("${mail.from:noreply@immosn.sn}")
    private String mailFrom;

    public ClientWelcomeNotificationServiceImpl(JavaMailSender mailSender, UserRepository userRepository) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }

    @Override
    public void sendCredentials(User user, String motDePasseTemporaire) {
        log.info("[NOTIFICATION] Envoi des identifiants au client converti : email={}", user.getEmail());
        envoyerEmailBienvenue(user, motDePasseTemporaire);
        envoyerSmsBienvenue(user);
        notifierSuperAdmins(user);
    }

    private void envoyerEmailBienvenue(User user, String motDePasseTemporaire) {
        if (!mailEnabled) {
            log.info("[EMAIL] Envoi désactivé — identifiants non envoyés par mail (destinataire={})", user.getEmail());
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(user.getEmail());
            message.setSubject("ImmoSN — Bienvenue, votre compte client a été créé");
            message.setText("Bonjour " + user.getNomComplet() + ",\n\n"
                + "Votre compte client ImmoSN a été créé.\n\n"
                + "Email de connexion : " + user.getEmail() + "\n"
                + "Mot de passe temporaire : " + motDePasseTemporaire + "\n\n"
                + "Pour des raisons de sécurité, vous devrez changer ce mot de passe dès votre première connexion.\n\n"
                + "L'équipe ImmoSN");
            mailSender.send(message);
            log.info("[EMAIL] Identifiants envoyés avec succès à {}", user.getEmail());
        } catch (Exception e) {
            log.error("[EMAIL] Échec d'envoi des identifiants à {} : {}", user.getEmail(), e.getMessage());
        }
    }

    private void envoyerSmsBienvenue(User user) {
        if (!twilioEnabled) {
            log.info("[SMS] Twilio désactivé — identifiants non envoyés par SMS (destinataire={})", maskTelephone(user.getTelephone()));
            return;
        }
        if (user.getTelephone() == null || user.getTelephone().isBlank()) {
            log.info("[SMS] Aucun téléphone renseigné pour {} — SMS de bienvenue ignoré", user.getEmail());
            return;
        }
        try {
            log.info("[SMS] Tentative d'envoi SMS de bienvenue à {}", maskTelephone(user.getTelephone()));
            Message.creator(
                new PhoneNumber(user.getTelephone()),
                new PhoneNumber(twilioFromNumber),
                "ImmoSN — Bienvenue ! Votre compte client a été créé. "
                    + "Consultez votre email pour vos identifiants de connexion."
            ).create();
            log.info("[SMS] SMS de bienvenue envoyé avec succès à {}", maskTelephone(user.getTelephone()));
        } catch (Exception e) {
            log.error("[SMS] Échec d'envoi du SMS de bienvenue à {} : {}", maskTelephone(user.getTelephone()), e.getMessage());
        }
    }

    private void notifierSuperAdmins(User nouveauClient) {
        if (!mailEnabled) {
            log.info("[EMAIL] Envoi désactivé — SUPER_ADMIN non notifié de la création du compte {}", nouveauClient.getEmail());
            return;
        }
        List<User> superAdmins = userRepository.findByRoles_Role(RoleType.SUPER_ADMIN, Pageable.unpaged()).getContent();
        if (superAdmins.isEmpty()) {
            log.warn("[EMAIL] Aucun SUPER_ADMIN trouvé — notification de création de compte non envoyée");
            return;
        }
        for (User admin : superAdmins) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(admin.getEmail());
                message.setSubject("ImmoSN — Nouveau compte client créé");
                message.setText("Un nouveau compte client a été créé.\n\n"
                    + "Nom : " + nouveauClient.getNomComplet() + "\n"
                    + "Email : " + nouveauClient.getEmail() + "\n"
                    + "Téléphone : " + (nouveauClient.getTelephone() != null ? nouveauClient.getTelephone() : "non renseigné") + "\n"
                    + "Type de conversion : Prospect → Client\n"
                    + "Date de création : " + nouveauClient.getCreationDate().format(DATE_FORMAT) + "\n\n"
                    + "L'équipe ImmoSN");
                mailSender.send(message);
                log.info("[EMAIL] SUPER_ADMIN {} notifié de la création du compte {}", admin.getEmail(), nouveauClient.getEmail());
            } catch (Exception e) {
                log.error("[EMAIL] Échec de notification du SUPER_ADMIN {} : {}", admin.getEmail(), e.getMessage());
            }
        }
    }

    private String maskTelephone(String telephone) {
        if (telephone == null || telephone.length() < 4) return "****";
        return "*".repeat(telephone.length() - 4) + telephone.substring(telephone.length() - 4);
    }
}
