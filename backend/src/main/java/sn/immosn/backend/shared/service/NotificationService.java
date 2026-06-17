package sn.immosn.backend.shared.service;

import sn.immosn.backend.auth.data.entity.User;

/**
 * Abstraction d'envoi de notifications (Sprint 3).
 *
 * <p>L'envoi réel (email/SMS) n'est pas encore branché : cette interface fige le contrat
 * pour permettre la conversion prospect → client dès maintenant. Une implémentation concrète
 * (SMTP, passerelle SMS…) pourra être fournie ultérieurement sans toucher au reste du code.</p>
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
