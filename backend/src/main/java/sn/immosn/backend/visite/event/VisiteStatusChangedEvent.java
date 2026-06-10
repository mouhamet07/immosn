package sn.immosn.backend.visite.event;

import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;

/**
 * Publié par DemandeVisiteServiceImpl à chaque transition de statut.
 *
 * source : "CLIENT" (création, annulation)
 *          "ADMIN"  (acceptation, refus, clôture, reprogrammation)
 *
 * Routing :
 *   source=CLIENT → broadcast à /topic/admin.notifications
 *   source=ADMIN  → sendToUser(clientEmail)
 */
public record VisiteStatusChangedEvent(
    Long                  visiteId,
    StatutDemandeVisite   ancienStatut,
    StatutDemandeVisite   nouveauStatut,
    String                clientEmail,
    Long                  clientId,
    String                source       // "CLIENT" | "ADMIN"
) {}
