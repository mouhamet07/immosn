package sn.immosn.backend.annonce.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.annonce.data.entity.TypeBienAnnonce;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.annonce.data.repository.CommoditeRepository;
import sn.immosn.backend.annonce.data.repository.TypeBienAnnonceRepository;
import sn.immosn.backend.annonce.service.Impl.AnnonceServiceImpl;
import sn.immosn.backend.client.web.annonce.dto.AnnonceCreateRequestDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceListDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceResponseDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceUpdateRequestDto;
import sn.immosn.backend.client.web.annonce.dto.SearchAnnonceRequestDto;
import sn.immosn.backend.client.web.annonce.mapper.AnnonceMapper;
import sn.immosn.backend.contrat.data.repository.ContratRepository;
import sn.immosn.backend.location.service.GeoCodingService;
import sn.immosn.backend.shared.exception.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnnonceService — Tests unitaires")
class AnnonceServiceTest {
    @Mock AnnonceRepository annonceRepository;
    @Mock CommoditeRepository commoditeRepository;
    @Mock TypeBienAnnonceRepository typeBienAnnonceRepository;
    @Mock AnnonceMapper annonceMapper;
    @Mock ContratRepository contratRepository; // requis par AnnonceServiceImpl (vérif archivage)
    @Mock GeoCodingService geoCodingService;
    @Captor ArgumentCaptor<Annonce> annonceCaptor;
    @InjectMocks
    AnnonceServiceImpl annonceService;
    private Annonce annonceActive;
    private AnnonceResponseDto responseDto;
    private AnnonceListDto     listDto;
    @BeforeEach
    void setUp() {
        TypeBienAnnonce type = new TypeBienAnnonce();
        type.setId(1L);
        type.setLibelle("Villa");
        annonceActive = Annonce.builder()
            .libelle("Villa Almadies")
            .description("Magnifique villa")
            .nbrPieces(5)
            .surface(250.0)
            .prix(BigDecimal.valueOf(150_000_000))
            .adresse("Almadies, Dakar")
            .typeBien(type)
            .isArchived(false)
            .build();
        // Simuler un ID généré
        java.lang.reflect.Field idField;
        try {
            idField = Annonce.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(annonceActive, 1L);
        } catch (Exception ignored) {}

        responseDto = new AnnonceResponseDto(
            1L, "Villa Almadies", "Magnifique villa", 5, 250.0,
            BigDecimal.valueOf(150_000_000), "Almadies, Dakar",
            null, null, null, null, null,
            null, List.of(), List.of(), false,
            LocalDateTime.now(), LocalDateTime.now()
        );

        listDto = new AnnonceListDto(
            "1", "Villa Almadies", BigDecimal.valueOf(150_000_000),
            "Almadies, Dakar", null, null, null, null,
            null, 5, 250.0, null, LocalDateTime.now(), false
        );
    }

    // ── getAnnonceById ──────────────────────────────────────

    @Test
    @DisplayName("getAnnonceById — annonce active trouvée")
    void getAnnonceById_found() {
        when(annonceRepository.findByIdAndIsArchivedFalse(1L))
            .thenReturn(Optional.of(annonceActive));
        when(annonceMapper.toResponse(annonceActive)).thenReturn(responseDto);

        AnnonceResponseDto result = annonceService.getAnnonceById(1L);

        assertThat(result).isNotNull();
        assertThat(result.libelle()).isEqualTo("Villa Almadies");
        verify(annonceRepository).findByIdAndIsArchivedFalse(1L);
    }

    @Test
    @DisplayName("getAnnonceById — annonce non trouvée → EntityNotFoundException")
    void getAnnonceById_notFound() {
        when(annonceRepository.findByIdAndIsArchivedFalse(99L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> annonceService.getAnnonceById(99L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("99");
    }

    // ── getAllAnnonces ──────────────────────────────────────

    @Test
    @DisplayName("getAllAnnonces — retourne une page de DTOs")
    void getAllAnnonces_returnsPage() {
        Page<Annonce> page = new PageImpl<>(List.of(annonceActive));
        when(annonceRepository.findByIsArchivedFalse(any())).thenReturn(page);
        when(annonceMapper.toListDto(annonceActive)).thenReturn(listDto);

        Page<AnnonceListDto> result = annonceService.getAllAnnonces(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).libelle()).isEqualTo("Villa Almadies");
    }

    @Test
    @DisplayName("createAnnonce — construit l'adresse finale avec adresse exacte")
    void createAnnonce_buildsFinalAdresseWithExactAddress() {
        AnnonceCreateRequestDto request = new AnnonceCreateRequestDto(
            "Villa Almadies",
            "Magnifique villa",
            5,
            250.0,
            BigDecimal.valueOf(150_000_000),
            "Route des Almadies",
            null,
            "Dakar",
            "Almadies",
            1L,
            null,
            null
        );

        TypeBienAnnonce typeBien = new TypeBienAnnonce();
        typeBien.setId(1L);
        when(typeBienAnnonceRepository.findByIdAndIsArchivedFalse(1L)).thenReturn(Optional.of(typeBien));
        when(commoditeRepository.findByIdInAndIsArchivedFalse(any())).thenReturn(List.of());

        Annonce incoming = Annonce.builder()
            .libelle(request.libelle())
            .description(request.description())
            .nbrPieces(request.nbrPieces())
            .surface(request.surface())
            .prix(request.prix())
            .region(request.region())
            .departement(request.departement())
            .quartier(request.quartier())
            .typeBien(typeBien)
            .isArchived(false)
            .build();

        when(annonceMapper.toEntity(any(), any(), any())).thenReturn(incoming);
        when(annonceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(annonceMapper.toResponse(any())).thenReturn(responseDto);
        when(geoCodingService.geocode("Almadies", "Dakar", "Route des Almadies")).thenReturn(Optional.of(new double[]{14.0, -17.0}));

        annonceService.createAnnonce(request);

        verify(annonceRepository).save(annonceCaptor.capture());
        assertThat(annonceCaptor.getValue().getAdresse()).isEqualTo("Route des Almadies, Almadies, Dakar");
    }

    @Test
    @DisplayName("createAnnonce — construit l'adresse finale sans adresse exacte")
    void createAnnonce_buildsFinalAdresseWithoutExactAddress() {
        AnnonceCreateRequestDto request = new AnnonceCreateRequestDto(
            "Villa Almadies",
            "Magnifique villa",
            5,
            250.0,
            BigDecimal.valueOf(150_000_000),
            null,
            null,
            "Dakar",
            "Almadies",
            1L,
            null,
            null
        );

        TypeBienAnnonce typeBien = new TypeBienAnnonce();
        typeBien.setId(1L);
        when(typeBienAnnonceRepository.findByIdAndIsArchivedFalse(1L)).thenReturn(Optional.of(typeBien));
        when(commoditeRepository.findByIdInAndIsArchivedFalse(any())).thenReturn(List.of());

        Annonce incoming = Annonce.builder()
            .libelle(request.libelle())
            .description(request.description())
            .nbrPieces(request.nbrPieces())
            .surface(request.surface())
            .prix(request.prix())
            .region(request.region())
            .departement(request.departement())
            .quartier(request.quartier())
            .typeBien(typeBien)
            .isArchived(false)
            .build();

        when(annonceMapper.toEntity(any(), any(), any())).thenReturn(incoming);
        when(annonceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(annonceMapper.toResponse(any())).thenReturn(responseDto);
        when(geoCodingService.geocode("Almadies", "Dakar", null)).thenReturn(Optional.of(new double[]{14.0, -17.0}));

        annonceService.createAnnonce(request);

        verify(annonceRepository).save(annonceCaptor.capture());
        assertThat(annonceCaptor.getValue().getAdresse()).isEqualTo("Almadies, Dakar");
    }

    @Test
    @DisplayName("updateAnnonce — recalcule l'adresse finale quand la localisation change sans adresse exacte")
    void updateAnnonce_recomputesFinalAdresseWhenLocationChangedWithoutExactAddress() {
        Annonce existing = Annonce.builder()
            .id(1L)
            .libelle("Villa Plateau")
            .description("Ancienne adresse")
            .nbrPieces(4)
            .surface(180.0)
            .prix(BigDecimal.valueOf(120_000_000))
            .adresse("Plateau, Dakar")
            .region("Dakar")
            .departement("Plateau")
            .quartier("Plateau")
            .typeBien(new TypeBienAnnonce())
            .isArchived(false)
            .build();

        when(annonceRepository.findByIdAndIsArchivedFalse(1L)).thenReturn(Optional.of(existing));
        when(annonceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(annonceMapper.toResponse(any())).thenReturn(responseDto);
        when(geoCodingService.geocode("Almadies", "Dakar", null)).thenReturn(Optional.of(new double[]{14.0, -17.0}));

        AnnonceUpdateRequestDto request = new AnnonceUpdateRequestDto(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "Dakar",
            "Almadies",
            null,
            null,
            null
        );

        annonceService.updateAnnonce(1L, request);

        verify(annonceRepository).save(annonceCaptor.capture());
        assertThat(annonceCaptor.getValue().getAdresse()).isEqualTo("Almadies, Dakar");
    }

    // ── archiveAnnonce ──────────────────────────────────────

    @Test
    @DisplayName("archiveAnnonce — met isArchived à true")
    void archiveAnnonce_setsArchivedTrue() {
        when(annonceRepository.findByIdAndIsArchivedFalse(1L))
            .thenReturn(Optional.of(annonceActive));
        when(annonceRepository.save(any())).thenReturn(annonceActive);

        annonceService.archiveAnnonce(1L);

        assertThat(annonceActive.isArchived()).isTrue();
        verify(annonceRepository).save(annonceActive);
    }

    @Test
    @DisplayName("archiveAnnonce — annonce inexistante → EntityNotFoundException")
    void archiveAnnonce_notFound() {
        when(annonceRepository.findByIdAndIsArchivedFalse(99L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> annonceService.archiveAnnonce(99L))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // ── restoreAnnonce ──────────────────────────────────────

    @Test
    @DisplayName("restoreAnnonce — met isArchived à false")
    void restoreAnnonce_setsArchivedFalse() {
        annonceActive.setArchived(true);
        when(annonceRepository.findById(1L)).thenReturn(Optional.of(annonceActive));
        when(annonceRepository.save(any())).thenReturn(annonceActive);

        annonceService.restoreAnnonce(1L);

        assertThat(annonceActive.isArchived()).isFalse();
    }

    // ── searchAnnonces ──────────────────────────────────────

    @Test
    @DisplayName("searchAnnonces — délègue au repository avec Specification")
    void searchAnnonces_delegatesToRepo() {
        SearchAnnonceRequestDto req = new SearchAnnonceRequestDto(
            null, null, null, "Almadies", null, null, 0, 9, "createdAt", "DESC"
        );
        Page<Annonce> page = new PageImpl<>(List.of(annonceActive));
        // Utiliser ArgumentMatchers.<Type>any() pour éviter les unchecked cast warnings
        when(annonceRepository.findAll(
            ArgumentMatchers.<Specification<Annonce>>any(),
            any(PageRequest.class))
        ).thenReturn(page);
        when(annonceMapper.toListDto(annonceActive)).thenReturn(listDto);

        Page<AnnonceListDto> result = annonceService.searchAnnonces(req);

        assertThat(result.getContent()).hasSize(1);
        verify(annonceRepository).findAll(
            ArgumentMatchers.<Specification<Annonce>>any(),
            any(PageRequest.class));
    }
}
