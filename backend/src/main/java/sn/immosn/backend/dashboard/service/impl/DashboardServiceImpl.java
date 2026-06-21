package sn.immosn.backend.dashboard.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.auth.data.entity.RoleType;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.shared.exception.EntityNotFoundException;
import sn.immosn.backend.client.web.dashboard.dto.DashboardStatsDto;
import sn.immosn.backend.client.web.dashboard.dto.RecentActivityDto;
import sn.immosn.backend.contrat.data.entity.StatutContrat;
import sn.immosn.backend.contrat.data.repository.ContratRepository;
import sn.immosn.backend.dashboard.service.DashboardService;
import sn.immosn.backend.discussion.data.repository.DiscussionRepository;
import sn.immosn.backend.lead.data.entity.StatutLead;
import sn.immosn.backend.lead.data.repository.LeadRepository;
import sn.immosn.backend.proprietaire.data.repository.ProprietaireRepository;
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

    private final AnnonceRepository annonceRepository;
    private final UserRepository userRepository;
    private final ContratRepository contratRepository;
    private final DemandeVisiteRepository visiteRepository;
    private final SignalementRepository signalementRepository;
    private final LeadRepository leadRepository;
    private final DiscussionRepository discussionRepository;
    private final ProprietaireRepository proprietaireRepository;

    @Override
    public DashboardStatsDto getStats(String callerEmail) {
        var page = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        Long adminId = adminScopeId(callerEmail);

        // Compteurs : COUNT uniquement, aucune entité chargée
        long totalAnnonces = annonceRepository.count();
        long annoncesActives = annonceRepository.countByIsArchivedFalse();
        long totalClients = userRepository.countByRoles_Role(RoleType.CLIENT);
        long totalAdmins = userRepository.countByRoles_Role(RoleType.ADMIN);
        long totalContrats = contratRepository.count();
        long contratsActifs = contratRepository.countByStatut(StatutContrat.ACTIF);
        long totalVisites = visiteRepository.count();
        long visitesEnAttente = visiteRepository.countByStatutAndIsArchivedFalse(StatutDemandeVisite.EN_ATTENTE);
        long visitesAujourdhui = countVisitesToday();
        long totalSig = signalementRepository.count();
        long sigOuverts = signalementRepository.countByStatut(StatutSignalement.OUVERT);
        long totalLeads = leadRepository.count();
        long leadsEnCours = leadRepository.countByStatut(StatutLead.EN_COURS);
        long leadsConvertis = leadRepository.countByStatut(StatutLead.CONVERTI);
        long leadsAbandonnes = leadRepository.countByStatut(StatutLead.ABANDONNE);
        long leadsTermines = leadsConvertis + leadsAbandonnes;
        double tauxConversionLeads = leadsTermines > 0
            ? Math.round(leadsConvertis * 10000.0 / leadsTermines) / 100.0
            : 0.0;
        long totalDisc = discussionRepository.count();

        // Sprint 4 — répartition par type de transaction
        long annoncesVente = annonceRepository.countByTypeTransactionAndIsArchivedFalse(
            sn.immosn.backend.annonce.data.entity.TypeTransaction.VENTE);
        long annoncesLocation = annonceRepository.countByTypeTransactionAndIsArchivedFalse(
            sn.immosn.backend.annonce.data.entity.TypeTransaction.LOCATION);
        long contratsVente = contratRepository.countByTypeContrat(
            sn.immosn.backend.contrat.data.entity.TypeContrat.VENTE);
        long contratsLocation = contratRepository.countByTypeContrat(
            sn.immosn.backend.contrat.data.entity.TypeContrat.LOCATION);
        long visitesVente = visiteRepository.countByAnnonce_TypeTransaction(
            sn.immosn.backend.annonce.data.entity.TypeTransaction.VENTE);
        long visitesLocation = visiteRepository.countByAnnonce_TypeTransaction(
            sn.immosn.backend.annonce.data.entity.TypeTransaction.LOCATION);

        // Module propriétaires
        long totalProprietaires = proprietaireRepository.count();
        long proprietairesActifs = proprietaireRepository.countByIsArchivedFalse();

        // Activités récentes (5 par domaine, fusionnées, priorité EN_ATTENTE/OUVERT en tête)
        List<RecentActivityDto> activites = buildRecentActivities(page, adminId);

        return new DashboardStatsDto(
            totalAnnonces, annoncesActives,
            totalClients, totalAdmins,
            totalContrats, contratsActifs,
            totalVisites, visitesEnAttente, visitesAujourdhui,
            totalSig, sigOuverts,
            totalLeads, leadsEnCours, leadsConvertis, leadsAbandonnes, tauxConversionLeads,
            totalDisc,
            annoncesVente, annoncesLocation, contratsVente, contratsLocation,
            visitesVente, visitesLocation,
            totalProprietaires, proprietairesActifs,
            activites
        );
    }

    @Override
    public PagedResponse<RecentActivityDto> getActivities(int page, int size, String type, String callerEmail) {
        String upper = type == null ? "ALL" : type.toUpperCase();
        var cap = PageRequest.of(0, BOUNDED_FETCH_CAP, Sort.by(Sort.Direction.DESC, "createdAt"));
        Long adminId = adminScopeId(callerEmail);

        List<RecentActivityDto> all = switch (upper) {
            case "VISITE" -> buildVisites(cap, adminId);
            case "CONTRAT" -> buildContrats(cap);
            case "SIGNALEMENT" -> buildSignalements(cap);
            case "BIEN" -> buildAnnonces(cap);
            case "MESSAGE" -> buildMessages(cap);
            default -> {
                List<RecentActivityDto> merged = new ArrayList<>();
                merged.addAll(buildAnnonces(cap));
                merged.addAll(buildVisites(cap, adminId));
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
        int totalPages = (int) Math.max(1, Math.ceil((double) totalElements / size));
        int fromIdx = page * size;
        int toIdx = Math.min(fromIdx + size, sorted.size());
        List<RecentActivityDto> content = fromIdx >= sorted.size() ? List.of() : sorted.subList(fromIdx, toIdx);

        return new PagedResponse<>(content, totalElements, totalPages, page, size,
            page == 0, page >= totalPages - 1);
    }

    private long countByType(String type) {
        return switch (type) {
            case "VISITE" -> visiteRepository.count();
            case "CONTRAT" -> contratRepository.count();
            case "SIGNALEMENT" -> signalementRepository.count();
            case "BIEN" -> annonceRepository.countByIsArchivedFalse();
            case "MESSAGE" -> discussionRepository.count();
            default -> annonceRepository.countByIsArchivedFalse()
                    + visiteRepository.count()
                    + contratRepository.count()
                    + signalementRepository.count()
                    + discussionRepository.count();
        };
    }

    private long countVisitesToday() {
        var start = LocalDate.now().atStartOfDay();
        var end = start.plusDays(1);
        return visiteRepository.countByDateVisiteBetweenAndIsArchivedFalse(start, end);
    }

    private List<RecentActivityDto> buildRecentActivities(PageRequest page, Long adminId) {
        List<RecentActivityDto> activities = new ArrayList<>();
        activities.addAll(buildAnnonces(page));
        activities.addAll(buildVisites(page, adminId));
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

    private List<RecentActivityDto> buildVisites(PageRequest page, Long adminId) {
        List<sn.immosn.backend.visite.data.entity.DemandeVisite> visites = adminId != null
            ? visiteRepository.findRecentForDashboardByAdmin(adminId, page)
            : visiteRepository.findRecentForDashboard(page);
        return visites.stream()
            .map(v -> new RecentActivityDto(
                "VISITE",
                "Visite : " + v.getAnnonce().getLibelle(),
                v.getClient().getNomComplet(),
                v.getStatut().name(), v.getCreatedAt()))
            .toList();
    }

    /** Retourne l'ID de l'admin connecté si ADMIN simple (filtrage requis), null si SUPER_ADMIN (vue globale). */
    private Long adminScopeId(String callerEmail) {
        User caller = userRepository.findByEmail(callerEmail)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
        boolean estSuperAdmin = caller.getRoles().stream()
            .anyMatch(r -> r.getRole() == RoleType.SUPER_ADMIN);
        return estSuperAdmin ? null : caller.getId();
    }

    private List<RecentActivityDto> buildContrats(PageRequest page) {
        return contratRepository.findRecentForDashboard(page).stream()
            .map(c -> new RecentActivityDto(
                "CONTRAT",
                "Contrat : " + c.getAnnonce().getLibelle(),
                c.getClient() != null ? c.getClient().getNomComplet() : nomProspect(c.getProspect()),
                c.getStatut().name(), c.getCreatedAt()))
            .toList();
    }

    private String nomProspect(sn.immosn.backend.prospect.data.entity.Prospect prospect) {
        if (prospect == null) return null;
        String prenom = prospect.getPrenom() != null && !prospect.getPrenom().isBlank() ? prospect.getPrenom().trim() + " " : "";
        return (prenom + (prospect.getNom() != null ? prospect.getNom() : "")).trim();
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
