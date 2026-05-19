package sn.immosn.backend.favoris.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sn.immosn.backend.client.web.favoris.dto.FavorisResponseDto;
import sn.immosn.backend.client.web.favoris.dto.FavorisStatusDto;

import java.util.List;

public interface FavorisService {

    /** Toggle favori — idempotent (ajoute si absent, retire si présent) */
    FavorisStatusDto toggle(Long annonceId, String clientEmail);

    /** Liste paginée des favoris avec détails (pour FavorisView) */
    Page<FavorisResponseDto> getClientFavoris(String clientEmail, Pageable pageable);

    /** Vérifie si une annonce spécifique est en favori */
    FavorisStatusDto checkFavoris(Long annonceId, String clientEmail);

    /**
     * Retourne tous les IDs d'annonces favorites du client.
     * Utilisé par le store Pinia pour pré-charger l'état ❤️ sans limite fixe.
     */
    List<Long> getAllFavorisIds(String clientEmail);
}
