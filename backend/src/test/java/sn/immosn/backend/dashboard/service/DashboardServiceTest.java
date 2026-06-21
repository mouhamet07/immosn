package sn.immosn.backend.dashboard.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.auth.data.entity.Role;
import sn.immosn.backend.auth.data.entity.RoleType;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.dashboard.dto.DashboardStatsDto;
import sn.immosn.backend.contrat.data.entity.StatutContrat;
import sn.immosn.backend.contrat.data.repository.ContratRepository;
import sn.immosn.backend.dashboard.service.impl.DashboardServiceImpl;
import sn.immosn.backend.discussion.data.repository.DiscussionRepository;
import sn.immosn.backend.lead.data.entity.StatutLead;
import sn.immosn.backend.lead.data.repository.LeadRepository;
import sn.immosn.backend.proprietaire.data.repository.ProprietaireRepository;
import sn.immosn.backend.signalement.data.entity.StatutSignalement;
import sn.immosn.backend.signalement.data.repository.SignalementRepository;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;
import sn.immosn.backend.visite.data.repository.DemandeVisiteRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService — Tests unitaires")
class DashboardServiceTest {

    @Mock AnnonceRepository annonceRepository;
    @Mock UserRepository userRepository;
    @Mock ContratRepository contratRepository;
    @Mock DemandeVisiteRepository visiteRepository;
    @Mock SignalementRepository signalementRepository;
    @Mock LeadRepository leadRepository;
    @Mock DiscussionRepository discussionRepository;
    @Mock ProprietaireRepository proprietaireRepository;

    @InjectMocks
    DashboardServiceImpl dashboardService;

    @Test
    @DisplayName("getStats — agrège les compteurs de tous les domaines")
    void getStats_aggregatesAllCounts() {
        User superAdmin = new User();
        superAdmin.setEmail("super@immosn.sn");
        superAdmin.setRoles(Set.of(new Role(1L, RoleType.SUPER_ADMIN)));
        when(userRepository.findByEmail("super@immosn.sn")).thenReturn(Optional.of(superAdmin));

        // --- Compteurs directs ---
        when(annonceRepository.count()).thenReturn(12L);
        when(annonceRepository.countByIsArchivedFalse()).thenReturn(8L);

        when(userRepository.countByRoles_Role(RoleType.CLIENT)).thenReturn(25L);
        when(userRepository.countByRoles_Role(RoleType.ADMIN)).thenReturn(3L);

        when(contratRepository.count()).thenReturn(7L);
        when(contratRepository.countByStatut(StatutContrat.ACTIF)).thenReturn(5L);

        when(visiteRepository.count()).thenReturn(30L);
        when(visiteRepository.countByStatutAndIsArchivedFalse(StatutDemandeVisite.EN_ATTENTE)).thenReturn(4L);
        when(visiteRepository.countByDateVisiteBetweenAndIsArchivedFalse(
            any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(2L);

        when(signalementRepository.count()).thenReturn(10L);
        when(signalementRepository.countByStatut(StatutSignalement.OUVERT)).thenReturn(2L);

        when(leadRepository.count()).thenReturn(15L);
        when(leadRepository.countByStatut(StatutLead.EN_COURS)).thenReturn(6L);
        when(leadRepository.countByStatut(StatutLead.CONVERTI)).thenReturn(4L);
        when(leadRepository.countByStatut(StatutLead.ABANDONNE)).thenReturn(2L);

        when(discussionRepository.count()).thenReturn(20L);

        // --- Activités récentes (retournent List, pas Page) ---
        when(annonceRepository.findByIsArchivedFalseOrderByCreatedAtDesc(any(Pageable.class)))
            .thenReturn(List.of());
        when(visiteRepository.findRecentForDashboard(any(Pageable.class)))
            .thenReturn(List.of());
        when(contratRepository.findRecentForDashboard(any(Pageable.class)))
            .thenReturn(List.of());
        when(signalementRepository.findRecentForDashboard(any(Pageable.class)))
            .thenReturn(List.of());
        when(discussionRepository.findTopRecentForDashboard(any(Pageable.class)))
            .thenReturn(List.of());

        // Exécution
        DashboardStatsDto stats = dashboardService.getStats("super@immosn.sn");

        // Vérifications
        assertThat(stats).isNotNull();
        assertThat(stats.totalAnnonces()).isEqualTo(12L);
        assertThat(stats.annoncesActives()).isEqualTo(8L);
        assertThat(stats.totalClients()).isEqualTo(25L);
        assertThat(stats.totalAdmins()).isEqualTo(3L);
        assertThat(stats.totalContrats()).isEqualTo(7L);
        assertThat(stats.contratsActifs()).isEqualTo(5L);
        assertThat(stats.totalVisites()).isEqualTo(30L);
        assertThat(stats.visitesEnAttente()).isEqualTo(4L);
        assertThat(stats.visitesAujourdhui()).isEqualTo(2L);
        assertThat(stats.totalSignalements()).isEqualTo(10L);
        assertThat(stats.signalementsOuverts()).isEqualTo(2L);
        assertThat(stats.totalLeads()).isEqualTo(15L);
        assertThat(stats.leadsEnCours()).isEqualTo(6L);
        assertThat(stats.leadsConvertis()).isEqualTo(4L);
        assertThat(stats.leadsAbandonnes()).isEqualTo(2L);
        assertThat(stats.tauxConversionLeads()).isEqualTo(66.67);
        assertThat(stats.totalDiscussions()).isEqualTo(20L);
        assertThat(stats.activitesRecentes()).isNotNull();
    }
}
