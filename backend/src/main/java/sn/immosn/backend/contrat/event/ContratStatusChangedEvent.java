package sn.immosn.backend.contrat.event;

import sn.immosn.backend.contrat.data.entity.StatutContrat;

/**
 * Publié par ContratServiceImpl et ContratExpirationJob à chaque transition de statut.
 *
 * source : "CLIENT" (demanderResiliation, demanderProlongation)
 *          "ADMIN"  (create, createFromVisite, accepter/refuser*)
 *          "SYSTEM" (ContratExpirationJob)
 *
 * Routing :
 *   source=CLIENT → broadcast à /topic/admin.notifications
 *   source=ADMIN ou SYSTEM → sendToUser(clientEmail)
 */
public record ContratStatusChangedEvent(
    Long          contratId,
    StatutContrat ancienStatut,
    StatutContrat nouveauStatut,
    String        clientEmail,
    Long          clientId,
    String        source       // "CLIENT" | "ADMIN" | "SYSTEM"
) {}
