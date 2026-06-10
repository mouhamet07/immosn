package sn.immosn.backend.shared.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Double filet au HikariCP keepalive : envoie un SELECT 1 toutes les 4 minutes
 * au niveau applicatif, indépendamment du pool.
 *
 * Neon free tier suspend le compute après 5 min sans SQL.
 * 240s (4min) << 300s (5min) → le compute ne dort jamais pendant l'exécution.
 *
 * fixedDelay (et non fixedRate) : le prochain tick démarre 4 min APRÈS la fin
 * du précédent — évite les chevauchements si Neon est lent à répondre.
 *
 * Actif uniquement sur le profil "neon" pour ne pas polluer les logs en local.
 */
@Component
@Profile("neon")
@RequiredArgsConstructor
@Slf4j
public class NeonHeartbeat {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedDelay = 240_000)
    public void ping() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            log.debug("[NEON] Heartbeat OK — compute actif");
        } catch (Exception e) {
            // Log uniquement — la prochaine requête utilisateur sera retentée par DatabaseRetryAspect
            log.warn("[NEON] Heartbeat échoué (Neon peut être en cold start) : {}", e.getMessage());
        }
    }
}
