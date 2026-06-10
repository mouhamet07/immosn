package sn.immosn.backend.discussion.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import sn.immosn.backend.websocket.dto.NotificationPayload;
import sn.immosn.backend.websocket.publisher.NotificationPublisher;

/**
 * Écoute les événements de messagerie et les route vers les bons destinataires.
 *
 * CLIENT envoie → notifie les ADMIN (broadcast admin topic)
 * ADMIN  envoie → notifie le CLIENT propriétaire de la discussion
 */
@Component
@RequiredArgsConstructor
public class DiscussionEventListener {

    private final NotificationPublisher publisher;

    @EventListener
    public void onMessageCreated(DiscussionMessageCreatedEvent event) {
        NotificationPayload payload = NotificationPayload.of(
            "NEW_MESSAGE",
            "Nouveau message",
            "Vous avez reçu un nouveau message dans la discussion #" + event.discussionId(),
            event.discussionId(),
            "DISCUSSION"
        );

        if ("CLIENT".equals(event.source())) {
            publisher.sendToAdmins(payload);
        } else {
            publisher.sendToUser(event.clientEmail(), payload);
        }
    }
}
