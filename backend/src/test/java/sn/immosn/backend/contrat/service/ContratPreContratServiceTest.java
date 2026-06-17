package sn.immosn.backend.contrat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.contrat.mapper.ContratMapper;
import sn.immosn.backend.contrat.data.entity.Contrat;
import sn.immosn.backend.contrat.data.entity.StatutContrat;
import sn.immosn.backend.contrat.data.repository.ContratRepository;
import sn.immosn.backend.contrat.service.impl.ContratServiceImpl;
import sn.immosn.backend.lead.data.repository.LeadRepository;
import sn.immosn.backend.lead.service.LeadHistoryService;
import sn.immosn.backend.shared.exception.EntityNotFoundException;
import sn.immosn.backend.shared.service.FileStorageService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContratService — circuit pré-contrat (Sprint 3)")
class ContratPreContratServiceTest {

    @Mock ContratRepository contratRepository;
    @Mock UserRepository userRepository;
    @Mock AnnonceRepository annonceRepository;
    @Mock LeadRepository leadRepository;
    @Mock ContratHistoryService contratHistoryService;
    @Mock LeadHistoryService leadHistoryService;
    @Mock FileStorageService fileStorageService;
    @org.mockito.Spy ContratMapper mapper = new ContratMapper();

    @InjectMocks ContratServiceImpl service;

    private User client;
    private Contrat contrat;

    @BeforeEach
    void setUp() {
        client = new User();
        client.setId(42L);
        client.setEmail("client@immosn.sn");
        client.setNomComplet("Aminata Diallo");

        var type = new sn.immosn.backend.annonce.data.entity.TypeBienAnnonce();
        type.setLibelle("Villa");
        var annonce = sn.immosn.backend.annonce.data.entity.Annonce.builder()
            .libelle("Villa F5").adresse("Almadies").typeBien(type).isArchived(false).build();

        contrat = Contrat.builder()
            .client(client).annonce(annonce)
            .statut(StatutContrat.EN_ATTENTE_VALIDATION_CLIENT)
            .montant(java.math.BigDecimal.valueOf(120_000_000))
            .build();
        contrat.setId(8L);
    }

    @Test
    @DisplayName("validerParClient — EN_ATTENTE_VALIDATION_CLIENT → EN_ATTENTE_VALIDATION_SUPER_ADMIN + horodatage")
    void validerParClient_ok() {
        when(userRepository.findByEmail("client@immosn.sn")).thenReturn(Optional.of(client));
        when(contratRepository.findByIdAndClientId(8L, 42L)).thenReturn(Optional.of(contrat));
        when(contratRepository.save(any(Contrat.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.validerParClient(8L, "client@immosn.sn");

        assertThat(dto.statut()).isEqualTo(StatutContrat.EN_ATTENTE_VALIDATION_SUPER_ADMIN);
        assertThat(contrat.getValideParClientAt()).isNotNull();
        verify(contratHistoryService).record(any(), eq(StatutContrat.EN_ATTENTE_VALIDATION_CLIENT),
            eq(StatutContrat.EN_ATTENTE_VALIDATION_SUPER_ADMIN), eq("VALIDATION_CLIENT"), any());
    }

    @Test
    @DisplayName("validerParClient — statut incompatible (ACTIF) → IllegalStateException")
    void validerParClient_wrongState() {
        contrat.setStatut(StatutContrat.ACTIF);
        when(userRepository.findByEmail("client@immosn.sn")).thenReturn(Optional.of(client));
        when(contratRepository.findByIdAndClientId(8L, 42L)).thenReturn(Optional.of(contrat));

        assertThatThrownBy(() -> service.validerParClient(8L, "client@immosn.sn"))
            .isInstanceOf(IllegalStateException.class);
        verify(contratRepository, never()).save(any());
    }

    @Test
    @DisplayName("activerParSuperAdmin — EN_ATTENTE_VALIDATION_SUPER_ADMIN → ACTIF + horodatage")
    void activerParSuperAdmin_ok() {
        contrat.setStatut(StatutContrat.EN_ATTENTE_VALIDATION_SUPER_ADMIN);
        when(contratRepository.findById(8L)).thenReturn(Optional.of(contrat));
        when(contratRepository.save(any(Contrat.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.activerParSuperAdmin(8L);

        assertThat(dto.statut()).isEqualTo(StatutContrat.ACTIF);
        assertThat(contrat.getValideParSuperAdminAt()).isNotNull();
        verify(contratHistoryService).record(any(), eq(StatutContrat.EN_ATTENTE_VALIDATION_SUPER_ADMIN),
            eq(StatutContrat.ACTIF), eq("ACTIVATION_SUPER_ADMIN"), any());
    }

    @Test
    @DisplayName("activerParSuperAdmin — pas encore validé par le client (BROUILLON) → IllegalStateException")
    void activerParSuperAdmin_notValidatedByClient() {
        contrat.setStatut(StatutContrat.BROUILLON);
        when(contratRepository.findById(8L)).thenReturn(Optional.of(contrat));

        assertThatThrownBy(() -> service.activerParSuperAdmin(8L))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("activerParSuperAdmin — contrat inexistant → EntityNotFoundException")
    void activerParSuperAdmin_notFound() {
        when(contratRepository.findById(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activerParSuperAdmin(8L))
            .isInstanceOf(EntityNotFoundException.class);
    }
}
