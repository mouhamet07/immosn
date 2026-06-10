package sn.immosn.backend.discussion.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import sn.immosn.backend.notification.entity.Notification;
import sn.immosn.backend.notification.service.NotificationService;
import sn.immosn.backend.websocket.dto.NotificationPayload;
import sn.immosn.backend.websocket.publisher.NotificationPublisher;

/**
 * Écoute les événements de messagerie et les route vers les bons destinataires.
 *
 * Ordre garanti : DB persistence → WebSocket push (best-effort).
 * Exécuté APRÈS le commit de la transaction métier (@TransactionalEventListener AFTER_COMMIT).
 *
 * CLIENT → notifie les ADMIN (broadcast admin topic)
 * ADMIN  → notifie le CLIENT propriétaire de la discussion
 */
@Component
@RequiredArgsConstructor
public class DiscussionEventListener {

    private final NotificationService notificationService;
    private final NotificationPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMessageCreated(DiscussionMessageCreatedEvent event) {
        String type    = "NEW_MESSAGE";
        String title   = "Nouveau message";
        String message = "Vous avez reçu un nouveau message dans la discussion #" + event.discussionId();

        if ("CLIENT".equals(event.source())) {
            notificationService.saveForAdmins(type, title, message, event.discussionId(), "DISCUSSION");
            publisher.sendToAdmins(NotificationPayload.of(type, title, message, event.discussionId(), "DISCUSSION"));
        } else {
            Notification n = notificationService.save(
                event.clientId(), type, title, message, event.discussionId(), "DISCUSSION"
            );
            publisher.sendToUser(event.clientEmail(), n);
        }
    }
}
