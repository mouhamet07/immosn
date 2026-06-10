package sn.immosn.backend.shared.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Déclenche le cold start Neon au démarrage du container Spring Boot, avant la
 * première requête utilisateur. Absorbe la latence 1–3s dans le temps de boot.
 *
 * Actif uniquement sur le profil "neon" pour ne pas interférer avec H2 en local.
 */
@Component
@Profile("neon")
@RequiredArgsConstructor
@Slf4j
public class NeonWarmup implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[NEON] Warmup — connexion au compute Neon...");
        long start = System.currentTimeMillis();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            log.info("[NEON] Warmup OK — compute actif ({}ms)", System.currentTimeMillis() - start);
        } catch (Exception e) {
            // Non bloquant : le DatabaseRetryAspect prend le relai sur la première requête réelle
            log.warn("[NEON] Warmup échoué — cold start sera payé sur la 1ère requête : {}", e.getMessage());
        }
    }
}
