package sn.immosn.backend.visite.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.auth.data.entity.Role;
import sn.immosn.backend.auth.data.entity.RoleType;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.visite.dto.AffecterAdminDto;
import sn.immosn.backend.client.web.visite.dto.DemandeVisiteResponseDto;
import sn.immosn.backend.client.web.visite.dto.ReplanificationDto;
import sn.immosn.backend.client.web.visite.mapper.DemandeVisiteMapper;
import sn.immosn.backend.lead.data.repository.LeadRepository;
import sn.immosn.backend.lead.service.LeadHistoryService;
import sn.immosn.backend.prospect.data.repository.ProspectRepository;
import sn.immosn.backend.shared.exception.EntityNotFoundException;
import sn.immosn.backend.visite.data.entity.DemandeVisite;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;
import sn.immosn.backend.visite.data.repository.DemandeVisiteRepository;
import sn.immosn.backend.visite.service.impl.DemandeVisiteServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DemandeVisiteService — workflow Sprint 2 (affectation, replanification)")
class DemandeVisiteWorkflowServiceTest {

    @Mock DemandeVisiteRepository visiteRepository;
    @Mock AnnonceRepository annonceRepository;
    @Mock UserRepository userRepository;
    @Mock ProspectRepository prospectRepository;
    @Mock LeadRepository leadRepository;
    @Mock sn.immosn.backend.contrat.service.ContratService contratService;
    @Mock VisiteHistoryService visiteHistoryService;
    @Mock LeadHistoryService leadHistoryService;
    @org.mockito.Spy DemandeVisiteMapper mapper = new DemandeVisiteMapper();

    @InjectMocks DemandeVisiteServiceImpl service;

    private DemandeVisite visite;
    private User admin;

    @BeforeEach
    void setUp() {
        var type = new sn.immosn.backend.annonce.data.entity.TypeBienAnnonce();
        type.setLibelle("Villa");
        var annonce = sn.immosn.backend.annonce.data.entity.Annonce.builder()
            .libelle("Villa F5").adresse("Almadies").typeBien(type).isArchived(false).build();

        visite = DemandeVisite.builder()
            .annonce(annonce)
            .dateVisite(LocalDateTime.now().plusDays(2))
            .statut(StatutDemandeVisite.ACCEPTEE)
            .build();
        visite.setId(50L);

        admin = new User();
        admin.setId(9L);
        admin.setEmail("admin@immosn.sn");
        admin.setNomComplet("Ibrahima Sow");
        admin.setRoles(Set.of(new Role(1L, RoleType.ADMIN)));
    }

    @Test
    @DisplayName("affecterAdmin — ACCEPTEE → AFFECTEE avec responsable")
    void affecterAdmin_acceptee_toAffectee() {
        when(visiteRepository.findByIdAndIsArchivedFalse(50L)).thenReturn(Optional.of(visite));
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
        when(visiteRepository.save(any(DemandeVisite.class))).thenAnswer(inv -> inv.getArgument(0));

        DemandeVisiteResponseDto dto = service.affecterAdmin(50L, new AffecterAdminDto(9L, "À traiter cette semaine"));

        assertThat(dto.statut()).isEqualTo(StatutDemandeVisite.AFFECTEE);
        assertThat(dto.adminResponsableId()).isEqualTo(9L);
        assertThat(dto.adminResponsableNom()).isEqualTo("Ibrahima Sow");
        verify(visiteHistoryService).record(any(), eq(StatutDemandeVisite.ACCEPTEE),
            eq(StatutDemandeVisite.AFFECTEE), eq("AFFECTATION"), any(), any(), any());
    }

    @Test
    @DisplayName("affecterAdmin — utilisateur cible non admin → IllegalArgumentException")
    void affecterAdmin_targetNotAdmin() {
        User client = new User();
        client.setId(11L);
        client.setRoles(Set.of(new Role(2L, RoleType.CLIENT)));
        when(visiteRepository.findByIdAndIsArchivedFalse(50L)).thenReturn(Optional.of(visite));
        when(userRepository.findById(11L)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.affecterAdmin(50L, new AffecterAdminDto(11L, null)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(visiteRepository, never()).save(any());
    }

    @Test
    @DisplayName("affecterAdmin — visite EN_ATTENTE (non acceptée) → IllegalStateException")
    void affecterAdmin_wrongState() {
        visite.setStatut(StatutDemandeVisite.EN_ATTENTE);
        when(visiteRepository.findByIdAndIsArchivedFalse(50L)).thenReturn(Optional.of(visite));

        assertThatThrownBy(() -> service.affecterAdmin(50L, new AffecterAdminDto(9L, null)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("demanderReplanification puis accepterReplanification — applique la nouvelle date")
    void replanification_flow() {
        LocalDateTime nouvelle = LocalDateTime.now().plusDays(5);
        visite.setAdminResponsable(admin);
        visite.setStatut(StatutDemandeVisite.AFFECTEE);
        when(visiteRepository.findByIdAndIsArchivedFalse(50L)).thenReturn(Optional.of(visite));
        when(visiteRepository.save(any(DemandeVisite.class))).thenAnswer(inv -> inv.getArgument(0));

        DemandeVisiteResponseDto afterDemande =
            service.demanderReplanification(50L, new ReplanificationDto(nouvelle, "Client indisponible"));
        assertThat(afterDemande.statut()).isEqualTo(StatutDemandeVisite.REPLANIFICATION_DEMANDEE);
        assertThat(afterDemande.dateReplanificationProposee()).isEqualTo(nouvelle);

        DemandeVisiteResponseDto afterAccept = service.accepterReplanification(50L);
        // Responsable assigné → retour à AFFECTEE, date appliquée, proposition effacée
        assertThat(afterAccept.statut()).isEqualTo(StatutDemandeVisite.AFFECTEE);
        assertThat(afterAccept.dateVisite()).isEqualTo(nouvelle);
        assertThat(afterAccept.dateReplanificationProposee()).isNull();
    }

    @Test
    @DisplayName("accepterReplanification — sans demande en cours → IllegalStateException")
    void accepterReplanification_noPending() {
        when(visiteRepository.findByIdAndIsArchivedFalse(50L)).thenReturn(Optional.of(visite)); // ACCEPTEE

        assertThatThrownBy(() -> service.accepterReplanification(50L))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("affecterAdmin — admin introuvable → EntityNotFoundException")
    void affecterAdmin_adminNotFound() {
        when(visiteRepository.findByIdAndIsArchivedFalse(50L)).thenReturn(Optional.of(visite));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.affecterAdmin(50L, new AffecterAdminDto(99L, null)))
            .isInstanceOf(EntityNotFoundException.class);
    }
}
