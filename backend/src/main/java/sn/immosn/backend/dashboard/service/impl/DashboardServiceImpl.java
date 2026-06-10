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
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.signalement.data.entity.StatutSignalement;
import sn.immosn.backend.signalement.data.repository.SignalementRepository;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;
import sn.immosn.backend.visite.data.repository.DemandeVisiteRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

// N+1 fix: toutes les associations LAZY dans les builders sont chargées via JOIN FETCH.
// Compteurs via countBy...() directs — pas de Page à getTotalElements() qui charge data+count+EAGER.

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final int BOUNDED_FETCH_CAP = 200;

    // Statuts qui nécessitent une action admin → affichés en priorité
    private static final Set<String> ACTION_REQUIRED_STATUS = Set.of("EN_ATTENTE", "OUVERT");

    private static final Comparator<RecentActivityDto> PRIORITY_COMPARATOR =
        Comparator.comparingInt((RecentActivityDto a) -> ACTION_REQUIRED_STATUS.contains(a.statut()) ? 0 : 1)
                  .thenComparing(Comparator.comparing(RecentActivityDto::createdAt).reversed());

    private final AnnonceRepository      annonceRepository;
    private final UserRepository         userRepository;
    private final ContratRepository      contratRepository;
    private final DemandeVisiteRepository visiteRepository;
    private final SignalementRepository  signalementRepository;
    private final LeadRepository         leadRepository;
    private final DiscussionRepository   discussionRepository;

    @Override
    public DashboardStatsDto getStats() {
        var page = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Compteurs : COUNT uniquement, aucune entité chargée
        long totalAnnonces     = annonceRepository.count();
        long annoncesActives   = annonceRepository.countByIsArchivedFalse();
        long totalClients      = userRepository.countByRoles_Role(RoleType.CLIENT);
        long totalAdmins       = userRepository.countByRoles_Role(RoleType.ADMIN);
        long totalContrats     = contratRepository.count();
        long contratsActifs    = contratRepository.countByStatut(StatutContrat.ACTIF);
        long totalVisites      = visiteRepository.count();
        long visitesEnAttente  = visiteRepository.countByStatutAndIsArchivedFalse(StatutDemandeVisite.EN_ATTENTE);
        long visitesAujourdhui = countVisitesToday();
        long totalSig          = signalementRepository.count();
        long sigOuverts        = signalementRepository.countByStatut(StatutSignalement.OUVERT);
        long totalLeads        = leadRepository.count();
        long leadsEnCours      = leadRepository.countByStatut(StatutLead.EN_COURS);
        long leadsConvertis    = leadRepository.countByStatut(StatutLead.CONVERTI);
        long leadsAbandonnes   = leadRepository.countByStatut(StatutLead.ABANDONNE);
        long leadsTermines     = leadsConvertis + leadsAbandonnes;
        double tauxConversionLeads = leadsTermines > 0
            ? Math.round(leadsConvertis * 10000.0 / leadsTermines) / 100.0
            : 0.0;
        long totalDisc = discussionRepository.count();

        // Activités récentes (5 par domaine, fusionnées, priorité EN_ATTENTE/OUVERT en tête)
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

    @Override
    public PagedResponse<RecentActivityDto> getActivities(int page, int size, String type) {
        String upper = type == null ? "ALL" : type.toUpperCase();
        var cap = PageRequest.of(0, BOUNDED_FETCH_CAP, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<RecentActivityDto> all = switch (upper) {
            case "VISITE"      -> buildVisites(cap);
            case "CONTRAT"     -> buildContrats(cap);
            case "SIGNALEMENT" -> buildSignalements(cap);
            case "BIEN"        -> buildAnnonces(cap);
            case "MESSAGE"     -> buildMessages(cap);
            default            -> {
                List<RecentActivityDto> merged = new ArrayList<>();
                merged.addAll(buildAnnonces(cap));
                merged.addAll(buildVisites(cap));
                merged.addAll(buildContrats(cap));
                merged.addAll(buildSignalements(cap));
                merged.addAll(buildMessages(cap));
                yield merged;
            }
        };

        List<RecentActivityDto> sorted = all.stream()
            .sorted(PRIORITY_COMPARATOR)
            .toList();

        long totalElements = countByType(upper);
        int  totalPages    = (int) Math.max(1, Math.ceil((double) totalElements / size));
        int  fromIdx       = page * size;
        int  toIdx         = Math.min(fromIdx + size, sorted.size());
        List<RecentActivityDto> content = fromIdx >= sorted.size() ? List.of() : sorted.subList(fromIdx, toIdx);

        return new PagedResponse<>(content, totalElements, totalPages, page, size,
            page == 0, page >= totalPages - 1);
    }

    private long countByType(String type) {
        return switch (type) {
            case "VISITE"      -> visiteRepository.count();
            case "CONTRAT"     -> contratRepository.count();
            case "SIGNALEMENT" -> signalementRepository.count();
            case "BIEN"        -> annonceRepository.countByIsArchivedFalse();
            case "MESSAGE"     -> discussionRepository.count();
            default            -> annonceRepository.countByIsArchivedFalse()
                                + visiteRepository.count()
                                + contratRepository.count()
                                + signalementRepository.count()
                                + discussionRepository.count();
        };
    }

    private long countVisitesToday() {
        var start = LocalDate.now().atStartOfDay();
        var end   = start.plusDays(1);
        return visiteRepository.countByDateVisiteBetweenAndIsArchivedFalse(start, end);
    }

    private List<RecentActivityDto> buildRecentActivities(PageRequest page) {
        List<RecentActivityDto> activities = new ArrayList<>();
        activities.addAll(buildAnnonces(page));
        activities.addAll(buildVisites(page));
        activities.addAll(buildContrats(page));
        activities.addAll(buildSignalements(page));
        activities.addAll(buildMessages(page));
        return activities.stream()
            .filter(a -> a.createdAt() != null)
            .sorted(PRIORITY_COMPARATOR)
            .limit(10)
            .toList();
    }

    private List<RecentActivityDto> buildAnnonces(PageRequest page) {
        return annonceRepository.findByIsArchivedFalseOrderByCreatedAtDesc(page).stream()
            .map(a -> new RecentActivityDto(
                "BIEN", a.getLibelle(), a.getAdresse(),
                "ACTIVE", a.getCreatedAt()))
            .toList();
    }

    private List<RecentActivityDto> buildVisites(PageRequest page) {
        return visiteRepository.findRecentForDashboard(page).stream()
            .map(v -> new RecentActivityDto(
                "VISITE",
                "Visite : " + v.getAnnonce().getLibelle(),
                v.getClient().getNomComplet(),
                v.getStatut().name(), v.getCreatedAt()))
            .toList();
    }

    private List<RecentActivityDto> buildContrats(PageRequest page) {
        return contratRepository.findRecentForDashboard(page).stream()
            .map(c -> new RecentActivityDto(
                "CONTRAT",
                "Contrat : " + c.getAnnonce().getLibelle(),
                c.getClient().getNomComplet(),
                c.getStatut().name(), c.getCreatedAt()))
            .toList();
    }

    private List<RecentActivityDto> buildSignalements(PageRequest page) {
        return signalementRepository.findRecentForDashboard(page).stream()
            .map(s -> new RecentActivityDto(
                "SIGNALEMENT",
                "Signalement SAV",
                s.getClient().getNomComplet(),
                s.getStatut().name(), s.getCreatedAt()))
            .toList();
    }

    private List<RecentActivityDto> buildMessages(PageRequest page) {
        return discussionRepository.findTopRecentForDashboard(page).stream()
            .map(d -> new RecentActivityDto(
                "MESSAGE",
                "Discussion : " + d.getAnnonce().getLibelle(),
                d.getClient().getNomComplet(),
                "ACTIF", d.getCreatedAt()))
            .toList();
    }
}
