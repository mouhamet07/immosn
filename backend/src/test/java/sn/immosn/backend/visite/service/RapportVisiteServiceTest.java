package sn.immosn.backend.visite.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.visite.dto.RapportVisiteCreateRequestDto;
import sn.immosn.backend.client.web.visite.dto.RapportVisiteResponseDto;
import sn.immosn.backend.client.web.visite.mapper.DemandeVisiteMapper;
import sn.immosn.backend.shared.exception.EntityNotFoundException;
import sn.immosn.backend.shared.service.FileStorageService;
import sn.immosn.backend.visite.data.entity.DemandeVisite;
import sn.immosn.backend.visite.data.entity.RapportVisite;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;
import sn.immosn.backend.visite.data.repository.DemandeVisiteRepository;
import sn.immosn.backend.visite.data.repository.RapportVisiteRepository;
import sn.immosn.backend.visite.service.impl.RapportVisiteServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RapportVisiteService — rédaction & consultation du rapport")
class RapportVisiteServiceTest {

    @Mock RapportVisiteRepository rapportRepository;
    @Mock DemandeVisiteRepository visiteRepository;
    @Mock UserRepository userRepository;
    @Mock FileStorageService fileStorageService;
    @Mock VisiteHistoryService visiteHistoryService;
    @org.mockito.Spy DemandeVisiteMapper mapper = new DemandeVisiteMapper();

    @InjectMocks RapportVisiteServiceImpl service;

    private DemandeVisite visite;
    private User admin;

    @BeforeEach
    void setUp() {
        visite = DemandeVisite.builder()
            .dateVisite(LocalDateTime.now().plusDays(1))
            .statut(StatutDemandeVisite.AFFECTEE)
            .build();
        visite.setId(60L);

        admin = new User();
        admin.setId(9L);
        admin.setEmail("admin@immosn.sn");
        admin.setNomComplet("Ibrahima Sow");
    }

    @Test
    @DisplayName("creerRapport — crée le rapport et passe la visite à RAPPORT_REDIGE")
    void creerRapport_setsStatutRapportRedige() {
        when(visiteRepository.findByIdAndIsArchivedFalse(60L)).thenReturn(Optional.of(visite));
        when(rapportRepository.existsByDemandeVisiteId(60L)).thenReturn(false);
        when(userRepository.findByEmail("admin@immosn.sn")).thenReturn(Optional.of(admin));
        when(rapportRepository.save(any(RapportVisite.class))).thenAnswer(inv -> {
            RapportVisite r = inv.getArgument(0);
            r.setId(3L);
            return r;
        });
        when(visiteRepository.save(any(DemandeVisite.class))).thenAnswer(inv -> inv.getArgument(0));

        RapportVisiteResponseDto dto = service.creerRapport(60L,
            new RapportVisiteCreateRequestDto("Visite effectuée, client très intéressé.", true),
            "admin@immosn.sn");

        assertThat(dto.id()).isEqualTo(3L);
        assertThat(dto.aboutie()).isTrue();
        assertThat(dto.auteurAdminNom()).isEqualTo("Ibrahima Sow");
        assertThat(visite.getStatut()).isEqualTo(StatutDemandeVisite.RAPPORT_REDIGE);
        verify(visiteHistoryService).record(any(), eq(StatutDemandeVisite.AFFECTEE),
            eq(StatutDemandeVisite.RAPPORT_REDIGE), eq("RAPPORT_REDIGE"), any(), any(), any());
    }

    @Test
    @DisplayName("creerRapport — rapport déjà existant → IllegalStateException")
    void creerRapport_alreadyExists() {
        when(visiteRepository.findByIdAndIsArchivedFalse(60L)).thenReturn(Optional.of(visite));
        when(rapportRepository.existsByDemandeVisiteId(60L)).thenReturn(true);

        assertThatThrownBy(() -> service.creerRapport(60L,
            new RapportVisiteCreateRequestDto("Doublon", false), "admin@immosn.sn"))
            .isInstanceOf(IllegalStateException.class);
        verify(rapportRepository, never()).save(any());
    }

    @Test
    @DisplayName("creerRapport — statut incompatible (EN_ATTENTE) → IllegalStateException")
    void creerRapport_wrongState() {
        visite.setStatut(StatutDemandeVisite.EN_ATTENTE);
        when(visiteRepository.findByIdAndIsArchivedFalse(60L)).thenReturn(Optional.of(visite));

        assertThatThrownBy(() -> service.creerRapport(60L,
            new RapportVisiteCreateRequestDto("Trop tôt", false), "admin@immosn.sn"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("uploadDocument — stocke le fichier et met à jour l'URL du rapport")
    void uploadDocument_storesAndUpdates() {
        RapportVisite rapport = RapportVisite.builder()
            .demandeVisite(visite).auteurAdmin(admin).compteRendu("ok").aboutie(true).build();
        rapport.setId(3L);
        when(rapportRepository.findByDemandeVisiteId(60L)).thenReturn(Optional.of(rapport));
        when(fileStorageService.store(any(), eq("rapports"))).thenReturn("/uploads/rapports/abc.pdf");
        when(rapportRepository.save(any(RapportVisite.class))).thenAnswer(inv -> inv.getArgument(0));

        var file = new org.springframework.mock.web.MockMultipartFile(
            "file", "r.pdf", "application/pdf", new byte[]{1, 2, 3});
        RapportVisiteResponseDto dto = service.uploadDocument(60L, file);

        assertThat(dto.documentUrl()).isEqualTo("/uploads/rapports/abc.pdf");
        ArgumentCaptor<RapportVisite> captor = ArgumentCaptor.forClass(RapportVisite.class);
        verify(rapportRepository).save(captor.capture());
        assertThat(captor.getValue().getDocumentUrl()).isEqualTo("/uploads/rapports/abc.pdf");
    }

    @Test
    @DisplayName("consulterRapport — visite sans rapport → EntityNotFoundException")
    void consulterRapport_notFound() {
        when(rapportRepository.findByDemandeVisiteId(60L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consulterRapport(60L))
            .isInstanceOf(EntityNotFoundException.class);
    }
}
