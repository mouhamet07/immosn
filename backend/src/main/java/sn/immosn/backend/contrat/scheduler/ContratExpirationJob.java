package sn.immosn.backend.contrat.scheduler;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.contrat.data.entity.Contrat;
import sn.immosn.backend.contrat.data.entity.StatutContrat;
import sn.immosn.backend.contrat.data.repository.ContratRepository;
import sn.immosn.backend.contrat.service.ContratHistoryService;

import java.time.LocalDate;
import java.util.List;

/**
 * Job planifié qui expire automatiquement les contrats dont la dateFin est dépassée.
 *
 * Exécution : tous les jours à minuit (configurable via CONTRAT_EXPIRATION_CRON).
 * La transaction garantit un rollback complet en cas d'erreur.
 */
@Component
@RequiredArgsConstructor
public class ContratExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(ContratExpirationJob.class);

    private final ContratRepository     contratRepository;
    private final ContratHistoryService contratHistoryService;

    /**
     * Passe les contrats ACTIF dont dateFin < aujourd'hui vers EXPIRE.
     * Cron : tous les jours à 00:05 (par défaut).
     * Override avec la variable d'env CONTRAT_EXPIRATION_CRON.
     * SecurityContext absent (job système) → auteurEmail = "SYSTEM".
     */
    @Scheduled(cron = "${contrat.expiration.cron:0 5 0 * * *}")
    @Transactional
    public void expireContrats() {
        LocalDate today = LocalDate.now();
        log.info("[ContratExpirationJob] Démarrage — vérification des contrats expirés au {}", today);

        List<Contrat> expired = contratRepository
            .findByStatutAndDateFinBefore(StatutContrat.ACTIF, today);

        if (expired.isEmpty()) {
            log.info("[ContratExpirationJob] Aucun contrat à expirer.");
            return;
        }

        expired.forEach(c -> {
            c.setStatut(StatutContrat.EXPIRE);
            log.info("[ContratExpirationJob] Contrat expiré : id={}, client={}, dateFin={}",
                c.getId(), c.getClient().getEmail(), c.getDateFin());
        });

        contratRepository.saveAll(expired);

        expired.forEach(c ->
            contratHistoryService.record(c, StatutContrat.ACTIF, StatutContrat.EXPIRE,
                "EXPIRATION_AUTOMATIQUE", "Expiration automatique au " + today)
        );

        log.info("[ContratExpirationJob] {} contrat(s) passé(s) en EXPIRE.", expired.size());
    }
}
