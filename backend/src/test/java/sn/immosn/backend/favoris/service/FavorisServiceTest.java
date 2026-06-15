package sn.immosn.backend.favoris.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.annonce.data.entity.TypeBienAnnonce;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.favoris.dto.FavorisResponseDto;
import sn.immosn.backend.client.web.favoris.dto.FavorisStatusDto;
import sn.immosn.backend.favoris.data.entity.AnnonceFavoris;
import sn.immosn.backend.favoris.data.repository.AnnonceFavorisRepository;
import sn.immosn.backend.favoris.service.impl.FavorisServiceImpl;
import sn.immosn.backend.shared.exception.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavorisService — Tests unitaires")
class FavorisServiceTest {

    @Mock AnnonceFavorisRepository favorisRepository;
    @Mock AnnonceRepository annonceRepository;
    @Mock UserRepository userRepository;

    @InjectMocks
    FavorisServiceImpl favorisService;

    private User client;
    private Annonce annonce;

    @BeforeEach
    void setUp() {
        client = new User();
        client.setId(1L);
        client.setEmail("client@test.sn");
        client.setNomComplet("Fatou Diallo");

        TypeBienAnnonce type = new TypeBienAnnonce();
        type.setLibelle("Appartement");

        annonce = Annonce.builder()
            .libelle("Appart Plateau")
            .prix(BigDecimal.valueOf(45_000_000))
            .adresse("Plateau, Dakar")
            .nbrPieces(3)
            .surface(90.0)
            .typeBien(type)
            .isArchived(false)
            .build();
        try {
            var f = Annonce.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(annonce, 2L);
        } catch (Exception ignored) {}
    }

    // toggle : ajout

    @Test
    @DisplayName("toggle — ajoute le favori si absent")
    void toggle_addsFavori() {
        when(userRepository.findByEmail("client@test.sn")).thenReturn(Optional.of(client));
        when(annonceRepository.findByIdAndIsArchivedFalse(2L)).thenReturn(Optional.of(annonce));
        when(favorisRepository.existsByClientIdAndAnnonceId(1L, 2L)).thenReturn(false);
        when(favorisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FavorisStatusDto result = favorisService.toggle(2L, "client@test.sn");

        assertThat(result.isFavoris()).isTrue();
        assertThat(result.annonceId()).isEqualTo(2L);
        verify(favorisRepository).save(any(AnnonceFavoris.class));
    }

    // toggle : retrait

    @Test
    @DisplayName("toggle — retire le favori si déjà présent")
    void toggle_removesFavori() {
        when(userRepository.findByEmail("client@test.sn")).thenReturn(Optional.of(client));
        when(annonceRepository.findByIdAndIsArchivedFalse(2L)).thenReturn(Optional.of(annonce));
        when(favorisRepository.existsByClientIdAndAnnonceId(1L, 2L)).thenReturn(true);
        doNothing().when(favorisRepository).deleteByClientIdAndAnnonceId(1L, 2L);

        FavorisStatusDto result = favorisService.toggle(2L, "client@test.sn");

        assertThat(result.isFavoris()).isFalse();
        verify(favorisRepository).deleteByClientIdAndAnnonceId(1L, 2L);
        verify(favorisRepository, never()).save(any());
    }

    // toggle : annonce non trouvée

    @Test
    @DisplayName("toggle — annonce inexistante → EntityNotFoundException")
    void toggle_annonceNotFound() {
        when(userRepository.findByEmail("client@test.sn")).thenReturn(Optional.of(client));
        when(annonceRepository.findByIdAndIsArchivedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favorisService.toggle(99L, "client@test.sn"))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // checkFavoris

    @Test
    @DisplayName("checkFavoris — retourne true si présent")
    void checkFavoris_returnsTrue() {
        when(userRepository.findByEmail("client@test.sn")).thenReturn(Optional.of(client));
        when(favorisRepository.existsActiveByClientIdAndAnnonceId(1L, 2L)).thenReturn(true);

        FavorisStatusDto result = favorisService.checkFavoris(2L, "client@test.sn");

        assertThat(result.isFavoris()).isTrue();
    }

    @Test
    @DisplayName("checkFavoris — retourne false si absent")
    void checkFavoris_returnsFalse() {
        when(userRepository.findByEmail("client@test.sn")).thenReturn(Optional.of(client));
        when(favorisRepository.existsActiveByClientIdAndAnnonceId(1L, 2L)).thenReturn(false);

        FavorisStatusDto result = favorisService.checkFavoris(2L, "client@test.sn");

        assertThat(result.isFavoris()).isFalse();
    }

    // getClientFavoris

    @Test
    @DisplayName("getClientFavoris — retourne une page de DTOs")
    void getClientFavoris_returnsPage() {
        AnnonceFavoris fav = AnnonceFavoris.builder()
            .client(client)
            .annonce(annonce)
            .build();
        // Simuler createdAt via réflexion
        try {
            var f = AnnonceFavoris.class.getDeclaredField("createdAt");
            f.setAccessible(true);
            f.set(fav, LocalDateTime.now());
        } catch (Exception ignored) {}

        when(userRepository.findByEmail("client@test.sn")).thenReturn(Optional.of(client));
        when(favorisRepository.findActiveByClientId(eq(1L), any()))
            .thenReturn(new PageImpl<>(List.of(fav)));

        Page<FavorisResponseDto> result = favorisService.getClientFavoris(
            "client@test.sn", PageRequest.of(0, 12));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).libelle()).isEqualTo("Appart Plateau");
    }
}
