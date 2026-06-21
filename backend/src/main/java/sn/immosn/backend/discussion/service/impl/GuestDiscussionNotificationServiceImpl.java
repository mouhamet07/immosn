package sn.immosn.backend.discussion.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import sn.immosn.backend.discussion.data.entity.Discussion;
import sn.immosn.backend.discussion.service.GuestDiscussionNotificationService;

/**
 * Envoi de l'email prévenant le visiteur (sans compte) qu'une réponse de l'agence
 * est disponible dans sa conversation. Échec d'envoi : loggé, jamais propagé —
 * la réponse admin reste enregistrée même si l'email échoue.
 */
@Slf4j
@Service
public class GuestDiscussionNotificationServiceImpl implements GuestDiscussionNotificationService {

    private final JavaMailSender mailSender;

    @Value("${mail.enabled:false}")
    private boolean mailEnabled;
    @Value("${mail.from:noreply@immosn.sn}")
    private String mailFrom;

    public GuestDiscussionNotificationServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void notifyGuestOfReply(Discussion discussion, String adminReplyContenu) {
        String guestEmail = discussion.getGuestEmail();
        if (guestEmail == null || guestEmail.isBlank()) {
            log.warn("[EMAIL] Discussion invité {} sans guestEmail — notification ignorée", discussion.getId());
            return;
        }
        if (!mailEnabled) {
            log.info("[EMAIL] Envoi désactivé — réponse agence non notifiée par mail (destinataire={})", guestEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(guestEmail);
            message.setSubject("ImmoSN — Réponse de l'agence à votre message");
            message.setText("Bonjour,\n\n"
                + "L'agence ImmoSN vous a répondu :\n\n"
                + "\"" + adminReplyContenu + "\"\n\n"
                + "Pour consulter et continuer cette conversation, retournez sur la page de l'annonce "
                + "concernée et utilisez le lien \"Reprendre ma conversation\".\n\n"
                + "L'équipe ImmoSN");
            mailSender.send(message);
            log.info("[EMAIL] Notification de réponse agence envoyée à {}", guestEmail);
        } catch (Exception e) {
            log.error("[EMAIL] Échec d'envoi de la notification de réponse à {} : {}", guestEmail, e.getMessage());
        }
    }
}
