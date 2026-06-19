package sn.immosn.backend.signalement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.annonce.data.entity.TypeBienAnnonce;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.signalement.dto.SignalementCreateRequestDto;
import sn.immosn.backend.client.web.signalement.dto.SignalementResponseDto;
import sn.immosn.backend.client.web.signalement.dto.UpdateStatutSignalementDto;
import sn.immosn.backend.client.web.signalement.mapper.SignalementMapper;
import sn.immosn.backend.contrat.data.entity.Contrat;
import sn.immosn.backend.contrat.data.entity.TypeContrat;
import sn.immosn.backend.contrat.data.repository.ContratRepository;
import sn.immosn.backend.signalement.data.entity.Signalement;
import sn.immosn.backend.signalement.data.entity.StatutSignalement;
import sn.immosn.backend.signalement.data.repository.SignalementRepository;
import sn.immosn.backend.signalement.service.impl.SignalementEligibiliteAnalyzer;
import sn.immosn.backend.signalement.service.impl.SignalementServiceImpl;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignalementService — éligibilité contractuelle (Sprint 4)")
class SignalementEligibiliteServiceTest {

    @Mock SignalementRepository signalementRepository;
    @Mock ContratRepository contratRepository;
    @Mock UserRepository userRepository;
    @Mock SignalementHistoryService historyService;
    @org.mockito.Spy SignalementMapper mapper = new SignalementMapper();
    // Analyseur réel : on teste les vraies règles métier
    @org.mockito.Spy SignalementEligibiliteAnalyzer analyzer = new SignalementEligibiliteAnalyzer();

    @InjectMocks SignalementServiceImpl service;

    private User client;

    @BeforeEach
    void setUp() {
        client = new User();
        client.setId(42L);
        client.setEmail("client@immosn.sn");
        client.setNomComplet("Aminata Diallo");
        // lenient : le test de rejet sans motif échoue avant l'appel à save()
        lenient().when(signalementRepository.save(any(Signalement.class))).thenAnswer(inv -> {
            Signalement s = inv.getArgument(0);
            if (s.getId() == null) s.setId(1L);
            return s;
        });
    }

    private Contrat contrat(TypeContrat type, LocalDate finGarantie) {
        TypeBienAnnonce tb = new TypeBienAnnonce();
        tb.setLibelle("Villa");
        Annonce annonce = Annonce.builder().libelle("Villa F5").adresse("Almadies").typeBien(tb).isArchived(false).build();
        Contrat c = Contrat.builder()
            .client(client).annonce(annonce).typeContrat(type)
            .dateFinGarantie(finGarantie)
            .build();
        c.setId(8L);
        return c;
    }

    @Test
    @DisplayName("VENTE hors garantie → NON_ELIGIBLE avec motif (refus automatique)")
    void vente_horsGarantie_nonEligible() {
        // Garantie expirée hier
        Contrat c = contrat(TypeContrat.VENTE, LocalDate.now().minusDays(1));
        when(userRepository.findByEmail("client@immosn.sn")).thenReturn(Optional.of(client));
        when(contratRepository.findByIdAndClientId(8L, 42L)).thenReturn(Optional.of(c));

        SignalementResponseDto dto = service.create(
            new SignalementCreateRequestDto(8L, "Fissure sur le mur porteur"), "client@immosn.sn");

        assertThat(dto.statut()).isEqualTo(StatutSignalement.NON_ELIGIBLE);
        assertThat(dto.motifRejet()).contains("après la fin de garantie");
        assertThat(dto.decisionAt()).isNotNull();
        // L'historique trace la décision de création
        verify(historyService).record(any(), isNull(), eq(StatutSignalement.NON_ELIGIBLE),
            eq("CREATION"), any(), eq("client@immosn.sn"));
    }

    @Test
    @DisplayName("VENTE pendant la garantie → EN_ANALYSE (éligible)")
    void vente_dansGarantie_enAnalyse() {
        Contrat c = contrat(TypeContrat.VENTE, LocalDate.now().plusMonths(6));
        when(userRepository.findByEmail("client@immosn.sn")).thenReturn(Optional.of(client));
        when(contratRepository.findByIdAndClientId(8L, 42L)).thenReturn(Optional.of(c));

        SignalementResponseDto dto = service.create(
            new SignalementCreateRequestDto(8L, "Problème de plomberie"), "client@immosn.sn");

        assertThat(dto.statut()).isEqualTo(StatutSignalement.EN_ANALYSE);
        assertThat(dto.motifRejet()).isNull();
    }

    @Test
    @DisplayName("VENTE sans garantie définie → NON_ELIGIBLE")
    void vente_sansGarantie_nonEligible() {
        Contrat c = contrat(TypeContrat.VENTE, null);
        when(userRepository.findByEmail("client@immosn.sn")).thenReturn(Optional.of(client));
        when(contratRepository.findByIdAndClientId(8L, 42L)).thenReturn(Optional.of(c));

        SignalementResponseDto dto = service.create(
            new SignalementCreateRequestDto(8L, "Souci divers"), "client@immosn.sn");

        assertThat(dto.statut()).isEqualTo(StatutSignalement.NON_ELIGIBLE);
        assertThat(dto.motifRejet()).contains("Aucune garantie");
    }

    @Test
    @DisplayName("LOCATION → EN_ANALYSE quelle que soit la date (analyse selon clauses)")
    void location_enAnalyse() {
        Contrat c = contrat(TypeContrat.LOCATION, null);
        when(userRepository.findByEmail("client@immosn.sn")).thenReturn(Optional.of(client));
        when(contratRepository.findByIdAndClientId(8L, 42L)).thenReturn(Optional.of(c));

        SignalementResponseDto dto = service.create(
            new SignalementCreateRequestDto(8L, "Dégradation de la porte d'entrée"), "client@immosn.sn");

        assertThat(dto.statut()).isEqualTo(StatutSignalement.EN_ANALYSE);
        assertThat(dto.motifRejet()).isNull();
        assertThat(dto.typeContrat()).isEqualTo(TypeContrat.LOCATION);
    }

    @Test
    @DisplayName("updateStatut REJETE sans motif → IllegalArgumentException")
    void rejet_sansMotif_refuse() {
        Signalement sig = Signalement.builder()
            .contrat(contrat(TypeContrat.LOCATION, null)).client(client)
            .contenu("x").statut(StatutSignalement.EN_ANALYSE).build();
        sig.setId(1L);
        when(signalementRepository.findById(1L)).thenReturn(Optional.of(sig));

        assertThatThrownBy(() -> service.updateStatut(1L,
            new UpdateStatutSignalementDto(StatutSignalement.REJETE, null, null)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateStatut REJETE avec motif → statut REJETE + motif + historique")
    void rejet_avecMotif_ok() {
        Signalement sig = Signalement.builder()
            .contrat(contrat(TypeContrat.LOCATION, null)).client(client)
            .contenu("x").statut(StatutSignalement.EN_ANALYSE).build();
        sig.setId(1L);
        when(signalementRepository.findById(1L)).thenReturn(Optional.of(sig));

        SignalementResponseDto dto = service.updateStatut(1L,
            new UpdateStatutSignalementDto(StatutSignalement.REJETE, null, "Usure normale, non couverte par le bail"));

        assertThat(dto.statut()).isEqualTo(StatutSignalement.REJETE);
        assertThat(dto.motifRejet()).isEqualTo("Usure normale, non couverte par le bail");
        ArgumentCaptor<StatutSignalement> cible = ArgumentCaptor.forClass(StatutSignalement.class);
        verify(historyService).record(any(), eq(StatutSignalement.EN_ANALYSE), cible.capture(),
            eq("CHANGEMENT_STATUT"), any(), isNull());
        assertThat(cible.getValue()).isEqualTo(StatutSignalement.REJETE);
    }
}
