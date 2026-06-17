package sn.immosn.backend.shared.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.shared.service.NotificationService;

/**
 * Implémentation par défaut : journalise l'envoi des identifiants (Sprint 3).
 *
 * <p>Aucun canal réel (email/SMS) n'est encore branché. Le mot de passe en clair n'est
 * volontairement PAS journalisé pour des raisons de sécurité ; seul l'email destinataire l'est.
 * Une implémentation concrète remplacera ce bean sans changement d'API.</p>
 */
@Slf4j
@Service
public class LoggingNotificationService implements NotificationService {

    @Override
    public void sendCredentials(User user, String motDePasseTemporaire) {
        // Ne jamais logger le mot de passe en clair.
        log.info("[NOTIFICATION] Identifiants à transmettre au client converti : email={} (mot de passe temporaire généré, à changer à la 1re connexion)",
            user.getEmail());
    }
}
