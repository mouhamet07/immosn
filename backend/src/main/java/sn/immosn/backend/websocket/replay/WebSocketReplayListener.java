package sn.immosn.backend.websocket.replay;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import sn.immosn.backend.notification.entity.Notification;
import sn.immosn.backend.notification.service.NotificationService;
import sn.immosn.backend.websocket.publisher.NotificationPublisher;
import sn.immosn.backend.websocket.security.WebSocketPrincipal;

import java.util.List;
import java.util.Map;

/**
 * Rejoue les notifications manquées dès que l'utilisateur s'abonne
 * à /user/queue/notifications (c'est-à-dire juste après la connexion WebSocket).
 *
 * Stratégie cursor :
 *  - Si lastSeenNotificationId est présent → envoie uniquement les notifications
 *    avec id > lastSeenNotificationId (déduplication parfaite).
 *  - Sinon → envoie toutes les non-lues des 7 derniers jours.
 *
 * Ce listener utilise @EventListener (pas @TransactionalEventListener) car
 * SessionSubscribeEvent est un événement WebSocket, pas un événement transactionnel.
 */
@Component
@RequiredArgsConstructor
public class WebSocketReplayListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketReplayListener.class);
    private static final String NOTIFICATIONS_DEST = "/user/queue/notifications";

    private final NotificationService   notificationService;
    private final NotificationPublisher notificationPublisher;

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());

        if (!NOTIFICATIONS_DEST.equals(headers.getDestination())) return;

        WebSocketPrincipal principal = (WebSocketPrincipal) headers.getUser();
        if (principal == null) return;

        Long lastSeenId = resolveLastSeenId(headers);
        List<Notification> missed = notificationService.getUnreadSince(principal.getUserId(), lastSeenId);

        if (missed.isEmpty()) return;

        log.info("[WS-REPLAY] {} notifications manquées → user={} (cursor={})",
            missed.size(), principal.getName(), lastSeenId);

        missed.forEach(n -> notificationPublisher.sendToUser(principal.getName(), n));
    }

    private Long resolveLastSeenId(StompHeaderAccessor headers) {
        Map<String, Object> attrs = headers.getSessionAttributes();
        if (attrs == null) return null;
        Object val = attrs.get("lastSeenNotificationId");
        return (val instanceof Long l) ? l : null;
    }
}
