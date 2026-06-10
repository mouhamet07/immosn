package sn.immosn.backend.websocket.dto;

import java.time.LocalDateTime;

/**
 * DTO unifié pour toutes les notifications WebSocket.
 *
 * type       : identifiant machine (ex. "VISITE_ACCEPTEE", "NEW_MESSAGE")
 * title      : titre lisible affiché dans la toast
 * message    : corps de la notification
 * entityId   : identifiant de l'entité concernée (pour navigation côté client)
 * entityType : "VISITE" | "CONTRAT" | "DISCUSSION" | "SIGNALEMENT"
 * timestamp  : instant de génération
 */
public record NotificationPayload(
    String type,
    String title,
    String message,
    Long entityId,
    String entityType,
    LocalDateTime timestamp
) {
    public static NotificationPayload of(
        String type, String title, String message, Long entityId, String entityType
    ) {
        return new NotificationPayload(type, title, message, entityId, entityType, LocalDateTime.now());
    }
}
