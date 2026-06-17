package sn.immosn.backend.visite.service;

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
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.visite.dto.VisiteInviteCreateRequestDto;
import sn.immosn.backend.client.web.visite.dto.VisiteInviteResponseDto;
import sn.immosn.backend.client.web.visite.mapper.DemandeVisiteMapper;
import sn.immosn.backend.lead.data.repository.LeadRepository;
import sn.immosn.backend.lead.service.LeadHistoryService;
import sn.immosn.backend.prospect.data.entity.Prospect;
import sn.immosn.backend.prospect.data.repository.ProspectRepository;
import sn.immosn.backend.shared.exception.EntityNotFoundException;
import sn.immosn.backend.visite.data.entity.DemandeVisite;
import sn.immosn.backend.visite.data.repository.DemandeVisiteRepository;
import sn.immosn.backend.visite.service.impl.DemandeVisiteServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DemandeVisiteService — création visite invité (parcours visiteur)")
class DemandeVisiteInviteServiceTest {

    @Mock DemandeVisiteRepository visiteRepository;
    @Mock AnnonceRepository annonceRepository;
    @Mock UserRepository userRepository;
    @Mock ProspectRepository prospectRepository;
    @Mock LeadRepository leadRepository;
    @Mock VisiteHistoryService visiteHistoryService;
    @Mock LeadHistoryService leadHistoryService;
    // Mapper réel : on vérifie le mapping invité de bout en bout
    @org.mockito.Spy DemandeVisiteMapper mapper = new DemandeVisiteMapper();

    @InjectMocks DemandeVisiteServiceImpl service;

    private Annonce annonce;

    @BeforeEach
    void setUp() {
        TypeBienAnnonce type = new TypeBienAnnonce();
        type.setLibelle("Villa");
        annonce = Annonce.builder()
            .libelle("Villa F5 - Almadies")
            .prix(BigDecimal.valueOf(120_000_000))
            .adresse("Route des Almadies")
            .nbrPieces(5)
            .surface(220.0)
            .typeBien(type)
            .isArchived(false)
            .build();
        try {
            var f = Annonce.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(annonce, 15L);
        } catch (Exception ignored) {}
    }

    private VisiteInviteCreateRequestDto request() {
        return new VisiteInviteCreateRequestDto(
            15L, "Diallo", "Aminata", "+221770000001", "aminata@email.com",
            "Almadies, Dakar", LocalDateTime.now().plusDays(3), "10:00",
            "Disponible en matinée"
        );
    }

    @Test
    @DisplayName("createInvite — crée un prospect (email inconnu) puis la demande de visite")
    void createInvite_createsProspectAndVisite() {
        when(annonceRepository.findByIdAndIsArchivedFalse(15L)).thenReturn(Optional.of(annonce));
        when(prospectRepository.findFirstByEmailOrderByCreatedAtAsc("aminata@email.com"))
            .thenReturn(Optional.empty());
        when(prospectRepository.save(any(Prospect.class))).thenAnswer(inv -> {
            Prospect p = inv.getArgument(0);
            p.setId(7L);
            return p;
        });
        when(visiteRepository.save(any(DemandeVisite.class))).thenAnswer(inv -> {
            DemandeVisite v = inv.getArgument(0);
            v.setId(99L);
            return v;
        });

        VisiteInviteResponseDto dto = service.createInvite(request());

        assertThat(dto.id()).isEqualTo(99L);
        assertThat(dto.nom()).isEqualTo("Diallo");
        assertThat(dto.email()).isEqualTo("aminata@email.com");
        assertThat(dto.prospectToken()).isNotBlank();
        assertThat(dto.annonceId()).isEqualTo(15L);

        // Le prospect porte un token UUID non vide, et la visite est rattachée au prospect (pas à un User)
        ArgumentCaptor<Prospect> prospectCaptor = ArgumentCaptor.forClass(Prospect.class);
        verify(prospectRepository).save(prospectCaptor.capture());
        assertThat(prospectCaptor.getValue().getToken()).isNotBlank();

        ArgumentCaptor<DemandeVisite> visiteCaptor = ArgumentCaptor.forClass(DemandeVisite.class);
        verify(visiteRepository).save(visiteCaptor.capture());
        assertThat(visiteCaptor.getValue().getClient()).isNull();
        assertThat(visiteCaptor.getValue().getProspect()).isNotNull();
        assertThat(visiteCaptor.getValue().getHeureVisite()).isEqualTo("10:00");

        // Aucun lead créé dans le flux invité (Lead reste lié à un User — sprint ultérieur)
        verifyNoInteractions(leadRepository);
        verifyNoInteractions(leadHistoryService);
    }

    @Test
    @DisplayName("createInvite — réutilise le prospect existant pour le même email")
    void createInvite_reusesExistingProspect() {
        Prospect existing = Prospect.builder()
            .nom("Diallo").email("aminata@email.com").telephone("+221770000001")
            .token("existing-token").build();
        existing.setId(7L);

        when(annonceRepository.findByIdAndIsArchivedFalse(15L)).thenReturn(Optional.of(annonce));
        when(prospectRepository.findFirstByEmailOrderByCreatedAtAsc("aminata@email.com"))
            .thenReturn(Optional.of(existing));
        when(visiteRepository.save(any(DemandeVisite.class))).thenAnswer(inv -> {
            DemandeVisite v = inv.getArgument(0);
            v.setId(100L);
            return v;
        });

        VisiteInviteResponseDto dto = service.createInvite(request());

        assertThat(dto.prospectToken()).isEqualTo("existing-token");
        verify(prospectRepository, never()).save(any());
    }

    @Test
    @DisplayName("createInvite — annonce inexistante → EntityNotFoundException")
    void createInvite_annonceNotFound() {
        when(annonceRepository.findByIdAndIsArchivedFalse(15L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createInvite(request()))
            .isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(visiteRepository);
    }
}
