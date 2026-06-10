package sn.immosn.backend.contrat.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import sn.immosn.backend.websocket.dto.NotificationPayload;
import sn.immosn.backend.websocket.publisher.NotificationPublisher;

/**
 * Route les événements de contrat vers les bons destinataires.
 *
 * source=CLIENT → notifie les ADMIN (demande résiliation/prolongation)
 * source=ADMIN  → notifie le CLIENT (décision admin)
 * source=SYSTEM → notifie le CLIENT (expiration automatique)
 */
@Component
@RequiredArgsConstructor
public class ContratEventListener {

    private final NotificationPublisher publisher;

    @EventListener
    public void onContratStatusChanged(ContratStatusChangedEvent event) {
        String type    = resolveType(event);
        String title   = resolveTitle(event);
        String message = resolveMessage(event);

        NotificationPayload payload = NotificationPayload.of(
            type, title, message, event.contratId(), "CONTRAT"
        );

        if ("CLIENT".equals(event.source())) {
            publisher.sendToAdmins(payload);
        } else {
            publisher.sendToUser(event.clientEmail(), payload);
        }
    }

    private String resolveType(ContratStatusChangedEvent e) {
        if (e.nouveauStatut() == null) return "CONTRAT_CREATED";
        return switch (e.nouveauStatut()) {
            case EN_ATTENTE                -> "CONTRAT_CREATED";
            case ACTIF                     -> resolveActifType(e);
            case RESILIE                   -> "CONTRAT_RESILIE";
            case EXPIRE                    -> "CONTRAT_EXPIRE";
            case EN_ATTENTE_RESILIATION    -> "CONTRAT_DEMANDE_RESILIATION";
            case PROLONGATION_EN_ATTENTE   -> "CONTRAT_DEMANDE_PROLONGATION";
        };
    }

    private String resolveActifType(ContratStatusChangedEvent e) {
        if (e.ancienStatut() == null) return "CONTRAT_CREATED";
        return switch (e.ancienStatut()) {
            case EN_ATTENTE_RESILIATION  -> "CONTRAT_RESILIATION_REFUSEE";
            case PROLONGATION_EN_ATTENTE -> "CONTRAT_PROLONGATION_ACCEPTEE";
            default                      -> "CONTRAT_ACTIVE";
        };
    }

    private String resolveTitle(ContratStatusChangedEvent e) {
        if (e.nouveauStatut() == null) return "Nouveau contrat";
        return switch (e.nouveauStatut()) {
            case EN_ATTENTE                -> "Nouveau contrat";
            case ACTIF                     -> resolveActifTitle(e);
            case RESILIE                   -> "Contrat résilié";
            case EXPIRE                    -> "Contrat expiré";
            case EN_ATTENTE_RESILIATION    -> "Demande de résiliation";
            case PROLONGATION_EN_ATTENTE   -> "Demande de prolongation";
        };
    }

    private String resolveActifTitle(ContratStatusChangedEvent e) {
        if (e.ancienStatut() == null) return "Contrat activé";
        return switch (e.ancienStatut()) {
            case EN_ATTENTE_RESILIATION  -> "Résiliation refusée";
            case PROLONGATION_EN_ATTENTE -> "Prolongation acceptée";
            default                      -> "Contrat activé";
        };
    }

    private String resolveMessage(ContratStatusChangedEvent e) {
        Long id = e.contratId();
        if (e.nouveauStatut() == null) return "Un nouveau contrat #" + id + " vous a été attribué.";
        return switch (e.nouveauStatut()) {
            case EN_ATTENTE                -> "Un nouveau contrat #" + id + " est en attente de validation.";
            case ACTIF                     -> resolveActifMessage(e);
            case RESILIE                   -> "Votre contrat #" + id + " a été résilié.";
            case EXPIRE                    -> "Votre contrat #" + id + " a expiré.";
            case EN_ATTENTE_RESILIATION    -> "Une demande de résiliation a été soumise pour le contrat #" + id + ".";
            case PROLONGATION_EN_ATTENTE   -> "Une demande de prolongation a été soumise pour le contrat #" + id + ".";
        };
    }

    private String resolveActifMessage(ContratStatusChangedEvent e) {
        Long id = e.contratId();
        if (e.ancienStatut() == null) return "Votre contrat #" + id + " a été activé.";
        return switch (e.ancienStatut()) {
            case EN_ATTENTE              -> "Votre contrat #" + id + " a été activé.";
            case EN_ATTENTE_RESILIATION  -> "Votre demande de résiliation pour le contrat #" + id + " a été refusée.";
            case PROLONGATION_EN_ATTENTE -> "Votre demande de prolongation pour le contrat #" + id + " a été acceptée.";
            default                      -> "Le statut du contrat #" + id + " a été mis à jour.";
        };
    }
}
