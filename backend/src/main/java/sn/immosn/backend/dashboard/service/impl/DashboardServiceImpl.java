package sn.immosn.backend.dashboard.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.auth.data.entity.RoleType;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.dashboard.dto.DashboardStatsDto;
import sn.immosn.backend.client.web.dashboard.dto.RecentActivityDto;
import sn.immosn.backend.contrat.data.entity.StatutContrat;
import sn.immosn.backend.contrat.data.repository.ContratRepository;
import sn.immosn.backend.dashboard.service.DashboardService;
import sn.immosn.backend.discussion.data.repository.DiscussionRepository;
import sn.immosn.backend.lead.data.entity.StatutLead;
import sn.immosn.backend.lead.data.repository.LeadRepository;
import sn.immosn.backend.signalement.data.entity.StatutSignalement;
import sn.immosn.backend.signalement.data.repository.SignalementRepository;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;
import sn.immosn.backend.visite.data.repository.DemandeVisiteRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// N+1 fix: toutes les associations LAZY dans buildRecentActivities() sont chargées via JOIN FETCH
// (visites.annonce, visites.client, contrats.annonce, contrats.client, signalements.client).
// Les compteurs utilisent countBy...() directs (plus de Page à getTotalElements() qui tire data+count+EAGER).

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final AnnonceRepository annonceRepository;
    private final UserRepository userRepository;
    private final ContratRepository contratRepository;
    private final DemandeVisiteRepository visiteRepository;
    private final SignalementRepository signalementRepository;
    private final LeadRepository leadRepository;
    private final DiscussionRepository discussionRepository;

    @Override
    public DashboardStatsDto getStats() {
        var page = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Compteurs : COUNT uniquement, aucune entité chargée
        long totalAnnonces    = annonceRepository.count();
        long annoncesActives  = annonceRepository.countByIsArchivedFalse();
        long totalClients     = userRepository.countByRoles_Role(RoleType.CLIENT);
        long totalAdmins      = userRepository.countByRoles_Role(RoleType.ADMIN);
        long totalContrats    = contratRepository.count();
        long contratsActifs   = contratRepository.countByStatut(StatutContrat.ACTIF);
        long totalVisites     = visiteRepository.count();
        long visitesEnAttente = visiteRepository.countByStatutAndIsArchivedFalse(StatutDemandeVisite.EN_ATTENTE);
        long visitesAujourdhui = countVisitesToday();
        long totalSig         = signalementRepository.count();
        long sigOuverts       = signalementRepository.countByStatut(StatutSignalement.OUVERT);
        long totalLeads       = leadRepository.count();
        long leadsEnCours     = leadRepository.countByStatut(StatutLead.EN_COURS);
        long leadsConvertis   = leadRepository.countByStatut(StatutLead.CONVERTI);
        long leadsAbandonnes  = leadRepository.countByStatut(StatutLead.ABANDONNE);
        long leadsTermines    = leadsConvertis + leadsAbandonnes;
        double tauxConversionLeads = leadsTermines > 0
            ? Math.round(leadsConvertis * 10000.0 / leadsTermines) / 100.0
            : 0.0;
        long totalDisc        = discussionRepository.count();

        // ── Activités récentes (5 dernières par domaine, fusionnées et triées) ─
        List<RecentActivityDto> activites = buildRecentActivities(page);

        return new DashboardStatsDto(
            totalAnnonces, annoncesActives,
            totalClients, totalAdmins,
            totalContrats, contratsActifs,
            totalVisites, visitesEnAttente, visitesAujourdhui,
            totalSig, sigOuverts,
            totalLeads, leadsEnCours, leadsConvertis, leadsAbandonnes, tauxConversionLeads,
            totalDisc,
            activites
        );
    }

    private long countVisitesToday() {
        var start = LocalDate.now().atStartOfDay();
        var end   = start.plusDays(1);
        return visiteRepository.countByDateVisiteBetweenAndIsArchivedFalse(start, end);
    }

    private List<RecentActivityDto> buildRecentActivities(PageRequest page) {
        List<RecentActivityDto> activities = new ArrayList<>();

        // Dernières annonces — champs directs uniquement, pas de JOIN nécessaire
        annonceRepository.findTop5ByIsArchivedFalseOrderByCreatedAtDesc().forEach(a ->
            activities.add(new RecentActivityDto(
                "ANNONCE", a.getLibelle(), a.getAdresse(),
                a.isArchived() ? "ARCHIVEE" : "ACTIVE", a.getCreatedAt())));

        // Dernières visites — JOIN FETCH v.annonce + v.client : 1 query au lieu de 1+5+5+5
        visiteRepository.findRecentForDashboard(page).forEach(v ->
            activities.add(new RecentActivityDto(
                "VISITE",
                "Visite : " + v.getAnnonce().getLibelle(),
                v.getClient().getNomComplet(),
                v.getStatut().name(), v.getCreatedAt())));

        // Derniers contrats — JOIN FETCH c.annonce + c.client : 1 query au lieu de 1+5+5+5
        contratRepository.findRecentForDashboard(page).forEach(c ->
            activities.add(new RecentActivityDto(
                "CONTRAT",
                "Contrat : " + c.getAnnonce().getLibelle(),
                c.getClient().getNomComplet(),
                c.getStatut().name(), c.getCreatedAt())));

        // Derniers signalements — JOIN FETCH s.client : 1 query au lieu de 1+5+5
        signalementRepository.findRecentForDashboard(page).forEach(s ->
            activities.add(new RecentActivityDto(
                "SIGNALEMENT",
                "Signalement SAV",
                s.getClient().getNomComplet(),
                s.getStatut().name(), s.getCreatedAt())));

        // Fusionner, trier par date décroissante, garder les 10 plus récentes
        return activities.stream()
            .filter(a -> a.createdAt() != null)
            .sorted(Comparator.comparing(RecentActivityDto::createdAt).reversed())
            .limit(10)
            .toList();
    }
}
