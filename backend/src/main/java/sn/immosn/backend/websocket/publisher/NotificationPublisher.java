package sn.immosn.backend.websocket.publisher;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import sn.immosn.backend.notification.entity.Notification;
import sn.immosn.backend.websocket.dto.NotificationPayload;

/**
 * Service central de publication WebSocket.
 * Seul point d'accès à SimpMessagingTemplate dans l'application.
 *
 * Les EventListeners sont les seuls appelants légitimes de ce service.
 * Les services métier publient des ApplicationEvents → les listeners appellent ici.
 *
 * sendToUser(email, payload)       : notification ciblée avec notificationId (déduplication)
 * sendToUser(email, notification)  : idem, à partir d'une entité DB
 * sendToAdmins(payload)            : broadcast /topic/admin.notifications (notificationId=null)
 */
@Service
@RequiredArgsConstructor
public class NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationPublisher.class);

    private static final String USER_QUEUE  = "/queue/notifications";
    private static final String ADMIN_TOPIC = "/topic/admin.notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(String email, NotificationPayload payload) {
        try {
            messagingTemplate.convertAndSendToUser(email, USER_QUEUE, payload);
            log.debug("[WS] → user={} type={} notifId={}", email, payload.type(), payload.notificationId());
        } catch (Exception e) {
            log.error("[WS] Erreur sendToUser user={} : {}", email, e.getMessage());
        }
    }

    /** Envoie une notification persistée en DB (notificationId inclus dans le payload). */
    public void sendToUser(String email, Notification notification) {
        NotificationPayload payload = new NotificationPayload(
            notification.getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getEntityId(),
            notification.getEntityType(),
            notification.getCreatedAt()
        );
        sendToUser(email, payload);
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
