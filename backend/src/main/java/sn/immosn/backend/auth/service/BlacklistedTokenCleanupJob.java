package sn.immosn.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.auth.data.repository.BlacklistedTokenRepository;

import java.time.LocalDateTime;

/**
 * Purge quotidienne des tokens JWT blacklistés expirés.
 * Sans ce job, la table blacklisted_tokens grossit indéfiniment
 * et ralentit le existsByToken() appelé à chaque requête HTTP.
 */
@Component
@RequiredArgsConstructor
public class BlacklistedTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(BlacklistedTokenCleanupJob.class);

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    // Tous les jours à 01:00 — après ContratExpirationJob (00:05)
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        int deleted = blacklistedTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("[BlacklistedTokenCleanupJob] {} token(s) expiré(s) supprimé(s).", deleted);
    }
}
