package sn.immosn.backend.discussion.event;

/**
 * Publié par DiscussionServiceImpl après chaque message sauvegardé.
 *
 * source      : "CLIENT" si le message vient du client, "ADMIN" sinon.
 * clientEmail : email du propriétaire de la discussion (toujours le CLIENT).
 *               Utilisé pour router la notification vers le bon utilisateur.
 */
public record DiscussionMessageCreatedEvent(
    Long   discussionId,
    Long   messageId,
    String contenu,
    String source,      // "CLIENT" | "ADMIN"
    String clientEmail,
    Long   clientId
) {}
