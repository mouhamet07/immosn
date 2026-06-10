package sn.immosn.backend.shared.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;

/**
 * Retry transparent sur les erreurs de connexion DB (Neon cold start / connexion morte).
 *
 * Intercepte tous les beans @Service. Sur CannotGetJdbcConnectionException ou
 * DataAccessResourceFailureException, retente jusqu'à 3 fois avec backoff 1s → 2s.
 * Toute autre exception remonte immédiatement.
 *
 * Si les 3 tentatives échouent, la dernière exception remonte vers
 * GlobalExceptionHandler qui la convertit en 503 propre.
 */
@Aspect
@Component
@Slf4j
public class DatabaseRetryAspect {

    private static final int MAX_ATTEMPTS = 4;
    // Neon free tier cold start : 5–10s. Backoff 2s+4s+6s = 12s de fenêtre totale.
    private static final long[] BACKOFF_MS = { 2_000L, 4_000L, 6_000L };

    @Around("within(@org.springframework.stereotype.Service *)")
    public Object retryOnConnectionFailure(ProceedingJoinPoint pjp) throws Throwable {
        int attempt = 0;
        while (true) {
            try {
                return pjp.proceed();
            } catch (DataAccessResourceFailureException ex) {
                attempt++;
                if (attempt >= MAX_ATTEMPTS) {
                    log.error("[NEON] Connexion DB échouée après {} tentatives sur {}.{}() : {}",
                        MAX_ATTEMPTS,
                        pjp.getTarget().getClass().getSimpleName(),
                        pjp.getSignature().getName(),
                        ex.getMessage());
                    throw ex;
                }
                long delay = BACKOFF_MS[Math.min(attempt - 1, BACKOFF_MS.length - 1)];
                log.warn("[NEON] Tentative {}/{} échouée sur {}.{}() — retry dans {}ms : {}",
                    attempt, MAX_ATTEMPTS,
                    pjp.getTarget().getClass().getSimpleName(),
                    pjp.getSignature().getName(),
                    delay,
                    ex.getMessage());
                Thread.sleep(delay);
            }
        }
    }
}
