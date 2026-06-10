package sn.immosn.backend.websocket.publisher;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import sn.immosn.backend.websocket.dto.NotificationPayload;

/**
 * Service central de publication WebSocket.
 * Seul point d'accès à SimpMessagingTemplate dans l'application.
 *
 * Les services métier NE doivent PAS appeler ce service directement :
 * ils publient des ApplicationEvents → les EventListeners appellent ce service.
 *
 * sendToUser()   : envoie à un utilisateur spécifique via /user/{email}/queue/notifications
 * sendToAdmins() : broadcast à /topic/admin.notifications (tous les admins connectés)
 */
@Service
@RequiredArgsConstructor
public class NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationPublisher.class);

    private static final String USER_QUEUE   = "/queue/notifications";
    private static final String ADMIN_TOPIC  = "/topic/admin.notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(String email, NotificationPayload payload) {
        try {
            messagingTemplate.convertAndSendToUser(email, USER_QUEUE, payload);
            log.debug("[WS] → user={} type={} entityId={}", email, payload.type(), payload.entityId());
        } catch (Exception e) {
            log.error("[WS] Erreur sendToUser user={} : {}", email, e.getMessage());
        }
    }

    public void sendToAdmins(NotificationPayload payload) {
        try {
            messagingTemplate.convertAndSend(ADMIN_TOPIC, payload);
            log.debug("[WS] → admins type={} entityId={}", payload.type(), payload.entityId());
        } catch (Exception e) {
            log.error("[WS] Erreur sendToAdmins : {}", e.getMessage());
        }
    }
}
