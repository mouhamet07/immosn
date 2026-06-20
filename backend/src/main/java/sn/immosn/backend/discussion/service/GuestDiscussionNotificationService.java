package sn.immosn.backend.discussion.service;

import sn.immosn.backend.discussion.data.entity.Discussion;

/**
 * Notification email du visiteur non authentifié (invité) dans une discussion.
 * Le visiteur n'a pas de compte : l'email est le seul canal pour le prévenir
 * qu'une réponse de l'agence est disponible.
 */
public interface GuestDiscussionNotificationService {

    void notifyGuestOfReply(Discussion discussion, String adminReplyContenu);
}
