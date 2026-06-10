package sn.immosn.backend.signalement.event;

/**
 * Publié par SignalementServiceImpl.updateStatut() quand l'admin répond.
 * Notifie le client de la réponse de l'administration.
 */
public record SignalementUpdatedEvent(
    Long    signalementId,
    String  nouveauStatut,
    String  clientEmail,
    Long    clientId,
    boolean hasReponse    // true si reponseAdmin est non null dans la mise à jour
) {}
