package sn.immosn.backend.signalement.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import sn.immosn.backend.websocket.dto.NotificationPayload;
import sn.immosn.backend.websocket.publisher.NotificationPublisher;

/**
 * Route les événements de signalement vers les bons destinataires.
 *
 * SignalementCreatedEvent : client crée → notifie les ADMIN
 * SignalementUpdatedEvent : admin répond → notifie le CLIENT
 */
@Component
@RequiredArgsConstructor
public class SignalementEventListener {

    private final NotificationPublisher publisher;

    @EventListener
    public void onSignalementCreated(SignalementCreatedEvent event) {
        NotificationPayload payload = NotificationPayload.of(
            "SIGNALEMENT_CREATED",
            "Nouveau signalement",
            "Un nouveau signalement #" + event.signalementId() + " a été soumis pour le contrat #" + event.contratId() + ".",
            event.signalementId(),
            "SIGNALEMENT"
        );
        publisher.sendToAdmins(payload);
    }

    @EventListener
    public void onSignalementUpdated(SignalementUpdatedEvent event) {
        if (!event.hasReponse()) return; // Ne notifie que quand l'admin a fourni une réponse

        NotificationPayload payload = NotificationPayload.of(
            "SIGNALEMENT_RESPONSE",
            "Réponse à votre signalement",
            "Votre signalement #" + event.signalementId() + " a reçu une réponse de l'administration.",
            event.signalementId(),
            "SIGNALEMENT"
        );
        publisher.sendToUser(event.clientEmail(), payload);
    }
}
