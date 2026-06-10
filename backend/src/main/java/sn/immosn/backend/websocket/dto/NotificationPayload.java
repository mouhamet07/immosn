package sn.immosn.backend.websocket.dto;

import java.time.LocalDateTime;

/**
 * DTO unifié pour toutes les notifications WebSocket.
 *
 * notificationId : id DB de la notification (null pour les broadcasts admin)
 *                  utilisé par le frontend pour la déduplication
 * type           : identifiant machine (ex. "VISITE_ACCEPTEE", "NEW_MESSAGE")
 * title          : titre lisible affiché dans la toast
 * message        : corps de la notification
 * entityId       : identifiant de l'entité concernée (pour navigation côté client)
 * entityType     : "VISITE" | "CONTRAT" | "DISCUSSION" | "SIGNALEMENT"
 * timestamp      : instant de génération
 */
public record NotificationPayload(
    Long notificationId,
    String type,
    String title,
    String message,
    Long entityId,
    String entityType,
    LocalDateTime timestamp
) {
    /** Notification ciblée — notificationId présent (déduplication active côté frontend). */
    public static NotificationPayload of(
        Long notificationId, String type, String title, String message, Long entityId, String entityType
    ) {
        return new NotificationPayload(notificationId, type, title, message, entityId, entityType, LocalDateTime.now());
    }

    /** Broadcast admin — notificationId null (chaque admin a son propre enregistrement en DB). */
    public static NotificationPayload of(
        String type, String title, String message, Long entityId, String entityType
    ) {
        return new NotificationPayload(null, type, title, message, entityId, entityType, LocalDateTime.now());
    }
}
