package sn.immosn.backend.shared.service;

import sn.immosn.backend.auth.data.entity.User;

/**
 * Abstraction d'envoi de notifications liées à la création de compte (Sprint 3).
 *
 * <p>Implémentation par défaut : {@link sn.immosn.backend.shared.service.impl.ClientWelcomeNotificationServiceImpl}
 * (email + SMS au client, email au(x) SUPER_ADMIN).</p>
 */
public interface NotificationService {

    /**
     * Transmet au nouvel utilisateur ses identifiants de connexion (email + mot de passe temporaire).
     *
     * @param user                 compte CLIENT fraîchement créé
     * @param motDePasseTemporaire  mot de passe en clair, à usage unique (l'utilisateur devra le changer)
     */
    void sendCredentials(User user, String motDePasseTemporaire);
}
