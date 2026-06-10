package sn.immosn.backend.signalement.event;

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
 * Route les événements de signalement vers les bons destinataires.
 * DB persistence → WebSocket push (best-effort), exécuté après commit.
 *
 * SignalementCreatedEvent : client crée → notifie les ADMIN
 * SignalementUpdatedEvent : admin répond → notifie le CLIENT
 */
@Component
@RequiredArgsConstructor
public class SignalementEventListener {

    private final NotificationService notificationService;
    private final NotificationPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSignalementCreated(SignalementCreatedEvent event) {
        String type    = "SIGNALEMENT_CREATED";
        String title   = "Nouveau signalement";
        String message = "Un nouveau signalement #" + event.signalementId()
            + " a été soumis pour le contrat #" + event.contratId() + ".";

        notificationService.saveForAdmins(type, title, message, event.signalementId(), "SIGNALEMENT");
        publisher.sendToAdmins(NotificationPayload.of(type, title, message, event.signalementId(), "SIGNALEMENT"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSignalementUpdated(SignalementUpdatedEvent event) {
        if (!event.hasReponse()) return;

        String type    = "SIGNALEMENT_RESPONSE";
        String title   = "Réponse à votre signalement";
        String message = "Votre signalement #" + event.signalementId()
            + " a reçu une réponse de l'administration.";

        Notification n = notificationService.save(
            event.clientId(), type, title, message, event.signalementId(), "SIGNALEMENT"
        );
        publisher.sendToUser(event.clientEmail(), n);
    }
}
