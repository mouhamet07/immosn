package sn.immosn.backend.signalement.event;

/**
 * Publié par SignalementServiceImpl.create().
 * Notifie les admins d'un nouveau signalement client.
 */
public record SignalementCreatedEvent(
    Long   signalementId,
    Long   contratId,
    String clientEmail,
    Long   clientId,
    String contenu
) {}
