package sn.immosn.backend.contrat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.annonce.data.entity.TypeBienAnnonce;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.auth.data.entity.Role;
import sn.immosn.backend.auth.data.entity.RoleType;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.contrat.dto.ContratUpdateRequestDto;
import sn.immosn.backend.client.web.contrat.mapper.ContratMapper;
import sn.immosn.backend.client.web.prospect.dto.ConversionResultDto;
import sn.immosn.backend.contrat.data.entity.Contrat;
import sn.immosn.backend.contrat.data.entity.StatutContrat;
import sn.immosn.backend.contrat.data.entity.TypeContrat;
import sn.immosn.backend.contrat.data.repository.ContratRepository;
import sn.immosn.backend.contrat.service.impl.ContratServiceImpl;
import sn.immosn.backend.lead.data.repository.LeadRepository;
import sn.immosn.backend.lead.service.LeadHistoryService;
import sn.immosn.backend.prospect.data.entity.Prospect;
import sn.immosn.backend.prospect.service.ProspectConversionService;
import sn.immosn.backend.shared.service.FileStorageService;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContratService — activation finale (validation SUPER_ADMIN) et création du compte client")
class ContratActivationServiceTest {

    @Mock ContratRepository contratRepository;
    @Mock UserRepository userRepository;
    @Mock AnnonceRepository annonceRepository;
    @Mock LeadRepository leadRepository;
    @Mock ContratHistoryService contratHistoryService;
    @Mock LeadHistoryService leadHistoryService;
    @Mock FileStorageService fileStorageService;
    @Mock ProspectConversionService prospectConversionService;
    @org.mockito.Spy ContratMapper mapper = new ContratMapper();

    @InjectMocks ContratServiceImpl service;

    private Annonce annonce;
    private Prospect prospect;
    private Contrat contrat;
    private User superAdmin;
    private User admin;

    @BeforeEach
    void setUp() {
        TypeBienAnnonce type = new TypeBienAnnonce();
        type.setLibelle("Villa");
        annonce = Annonce.builder()
            .libelle("Villa F5").adresse("Almadies").typeBien(type).isArchived(false).build();

        prospect = Prospect.builder()
            .nom("Diallo").prenom("Aminata").email("aminata@email.com")
            .telephone("+221770000001").token("tok-1").build();
        prospect.setId(7L);

        contrat = Contrat.builder()
            .prospect(prospect).annonce(annonce)
            .statut(StatutContrat.EN_ATTENTE)
            .typeContrat(TypeContrat.VENTE)
            .montant(BigDecimal.valueOf(120_000_000))
            .build();
        contrat.setId(8L);

        superAdmin = new User();
        superAdmin.setEmail("super@immosn.sn");
        superAdmin.setRoles(Set.of(new Role(1L, RoleType.SUPER_ADMIN)));

        admin = new User();
        admin.setEmail("admin@immosn.sn");
        admin.setRoles(Set.of(new Role(2L, RoleType.ADMIN)));
    }

    private ContratUpdateRequestDto activationRequest(String documentUrl) {
        return new ContratUpdateRequestDto(null, null, null, null,
            StatutContrat.ACTIF, documentUrl, null, null, null);
    }

    @Test
    @DisplayName("update(ACTIF) par un ADMIN simple (non SUPER_ADMIN) → AccessDeniedException, aucune écriture")
    void activation_parAdminSimple_rejetee() {
        when(contratRepository.findById(8L)).thenReturn(Optional.of(contrat));
        when(userRepository.findByEmail("admin@immosn.sn")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.update(8L, activationRequest("https://cdn/contrat-signe.pdf"), "admin@immosn.sn"))
            .isInstanceOf(AccessDeniedException.class);

        verify(contratRepository, never()).save(any());
        verifyNoInteractions(prospectConversionService);
    }

    @Test
    @DisplayName("update(ACTIF) sans document signé (ni en base ni dans la requête) → IllegalArgumentException, aucune écriture")
    void activation_sansDocument_rejetee() {
        when(contratRepository.findById(8L)).thenReturn(Optional.of(contrat));
        when(userRepository.findByEmail("super@immosn.sn")).thenReturn(Optional.of(superAdmin));

        assertThatThrownBy(() -> service.update(8L, activationRequest(null), "super@immosn.sn"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("téléversé");

        verify(contratRepository, never()).save(any());
        verifyNoInteractions(prospectConversionService);
    }

    @Test
    @DisplayName("update(ACTIF) par SUPER_ADMIN avec document signé et contrat sans client (prospect) → crée le compte client et pose valideParSuperAdminAt")
    void activation_avecDocumentEtProspect_creeLeCompteClient() {
        User client = new User();
        client.setId(99L);
        client.setEmail("aminata@email.com");
        client.setNomComplet("Aminata Diallo");

        when(contratRepository.findById(8L)).thenReturn(Optional.of(contrat));
        when(userRepository.findByEmail("super@immosn.sn")).thenReturn(Optional.of(superAdmin));
        when(prospectConversionService.convertirProspect(7L)).thenReturn(
            new ConversionResultDto(7L, 99L, "aminata@email.com", "Aminata Diallo",
                true, true, false, 0, 1));
        when(userRepository.findById(99L)).thenReturn(Optional.of(client));
        when(contratRepository.save(any(Contrat.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.update(8L, activationRequest("https://cdn/contrat-signe.pdf"), "super@immosn.sn");

        assertThat(dto.statut()).isEqualTo(StatutContrat.ACTIF);
        assertThat(dto.clientId()).isEqualTo(99L);
        assertThat(contrat.getClient()).isEqualTo(client);
        assertThat(contrat.getValideParSuperAdminAt()).isNotNull();
        verify(prospectConversionService).convertirProspect(7L);
        verify(contratHistoryService).record(any(), eq(StatutContrat.EN_ATTENTE),
            eq(StatutContrat.ACTIF), any(), any());
    }

    @Test
    @DisplayName("update(ACTIF) quand le contrat a déjà un client connu → ne déclenche pas de conversion")
    void activation_clientDejaPresent_neConvertitPas() {
        User client = new User();
        client.setId(42L);
        client.setEmail("client@immosn.sn");
        client.setNomComplet("Client Existant");

        contrat.setProspect(null);
        contrat.setClient(client);

        when(contratRepository.findById(8L)).thenReturn(Optional.of(contrat));
        when(userRepository.findByEmail("super@immosn.sn")).thenReturn(Optional.of(superAdmin));
        when(contratRepository.save(any(Contrat.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.update(8L, activationRequest("https://cdn/contrat-signe.pdf"), "super@immosn.sn");

        assertThat(dto.statut()).isEqualTo(StatutContrat.ACTIF);
        assertThat(dto.clientId()).isEqualTo(42L);
        verifyNoInteractions(prospectConversionService);
    }

    @Test
    @DisplayName("update(ACTIF) avec documentUrl déjà présent en base (pas dans la requête) → activation autorisée")
    void activation_documentDejaEnBase_autorisee() {
        contrat.setDocumentUrl("https://cdn/contrat-deja-uploade.pdf");
        User client = new User();
        client.setId(99L);
        client.setEmail("aminata@email.com");
        client.setNomComplet("Aminata Diallo");

        when(contratRepository.findById(8L)).thenReturn(Optional.of(contrat));
        when(userRepository.findByEmail("super@immosn.sn")).thenReturn(Optional.of(superAdmin));
        when(prospectConversionService.convertirProspect(7L)).thenReturn(
            new ConversionResultDto(7L, 99L, "aminata@email.com", "Aminata Diallo",
                true, true, false, 0, 1));
        when(userRepository.findById(99L)).thenReturn(Optional.of(client));
        when(contratRepository.save(any(Contrat.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.update(8L, activationRequest(null), "super@immosn.sn");

        assertThat(dto.statut()).isEqualTo(StatutContrat.ACTIF);
    }
}
