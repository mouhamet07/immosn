package sn.immosn.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.auth.data.repository.BlacklistedTokenRepository;
import sn.immosn.backend.auth.data.repository.UserSessionRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class BlacklistedTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(BlacklistedTokenCleanupJob.class);
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final UserSessionRepository      userSessionRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int deletedTokens   = blacklistedTokenRepository.deleteExpiredTokens(now);
        int deletedSessions = userSessionRepository.deleteExpiredSessions(now);
        log.info("[BlacklistedTokenCleanupJob] {} token(s) et {} session(s) expiré(s) supprimé(s).",
            deletedTokens, deletedSessions);
    }
}
