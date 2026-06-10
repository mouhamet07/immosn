package sn.immosn.backend.visite.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import sn.immosn.backend.notification.entity.Notification;
import sn.immosn.backend.notification.service.NotificationService;
import sn.immosn.backend.websocket.dto.NotificationPayload;
import sn.immosn.backend.websocket.publisher.NotificationPublisher;

/**
 * Route les événements de visite vers les bons destinataires.
 * DB persistence → WebSocket push (best-effort), exécuté après commit.
 *
 * source=CLIENT → notifie les ADMIN
 * source=ADMIN  → notifie le CLIENT concerné
 */
@Component
@RequiredArgsConstructor
public class VisiteEventListener {

    private final NotificationService notificationService;
    private final NotificationPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional
    public void onVisiteStatusChanged(VisiteStatusChangedEvent event) {
        String type    = resolveType(event);
        String title   = resolveTitle(event);
        String message = resolveMessage(event);

        if ("CLIENT".equals(event.source())) {
            notificationService.saveForAdmins(type, title, message, event.visiteId(), "VISITE");
            publisher.sendToAdmins(NotificationPayload.of(type, title, message, event.visiteId(), "VISITE"));
        } else {
            Notification n = notificationService.save(
                event.clientId(), type, title, message, event.visiteId(), "VISITE"
            );
            publisher.sendToUser(event.clientEmail(), n);
        }
    }

    private String resolveType(VisiteStatusChangedEvent e) {
        if (e.nouveauStatut() == null) return "VISITE_CREATED";
        return switch (e.nouveauStatut()) {
            case EN_ATTENTE                -> "VISITE_CREATED";
            case ACCEPTEE                  -> "VISITE_ACCEPTEE";
            case REFUSEE                   -> "VISITE_REFUSEE";
            case ANNULEE                   -> "VISITE_ANNULEE";
            case CLOTUREE_SANS_SUITE       -> "VISITE_CLOTUREE_SANS_SUITE";
            case CLOTUREE_AVEC_CONTRAT     -> "VISITE_CLOTUREE_AVEC_CONTRAT";
            default                        -> "VISITE_STATUS_CHANGED";
        };
    }

    private String resolveTitle(VisiteStatusChangedEvent e) {
        if (e.nouveauStatut() == null) return "Nouvelle demande de visite";
        return switch (e.nouveauStatut()) {
            case EN_ATTENTE                -> "Nouvelle demande de visite";
            case ACCEPTEE                  -> "Visite acceptée";
            case REFUSEE                   -> "Visite refusée";
            case ANNULEE                   -> "Visite annulée";
            case CLOTUREE_SANS_SUITE       -> "Visite clôturée";
            case CLOTUREE_AVEC_CONTRAT     -> "Contrat créé suite à votre visite";
            default                        -> "Mise à jour de visite";
        };
    }

    private String resolveMessage(VisiteStatusChangedEvent e) {
        if (e.nouveauStatut() == null)
            return "Une nouvelle demande de visite #" + e.visiteId() + " a été soumise.";
        return switch (e.nouveauStatut()) {
            case EN_ATTENTE            -> "Nouvelle demande de visite #" + e.visiteId() + " reçue.";
            case ACCEPTEE              -> "Votre demande de visite #" + e.visiteId() + " a été acceptée.";
            case REFUSEE               -> "Votre demande de visite #" + e.visiteId() + " a été refusée.";
            case ANNULEE               -> "La demande de visite #" + e.visiteId() + " a été annulée.";
            case CLOTUREE_SANS_SUITE   -> "Votre visite #" + e.visiteId() + " a été clôturée sans suite.";
            case CLOTUREE_AVEC_CONTRAT -> "Un contrat a été créé à l'issue de votre visite #" + e.visiteId() + ".";
            default                    -> "Le statut de la visite #" + e.visiteId() + " a changé.";
        };
    }
}
