package sn.immosn.backend.favoris.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sn.immosn.backend.client.web.favoris.dto.FavorisResponseDto;
import sn.immosn.backend.client.web.favoris.dto.FavorisStatusDto;

public interface FavorisService {

    /** Ajouter une annonce aux favoris — idempotent (ne lève pas d'erreur si déjà présent) */
    FavorisStatusDto toggle(Long annonceId, String clientEmail);

    /** Liste des favoris du client connecté, paginée */
    Page<FavorisResponseDto> getClientFavoris(String clientEmail, Pageable pageable);

    /** Vérifier si une annonce est dans les favoris du client */
    FavorisStatusDto checkFavoris(Long annonceId, String clientEmail);
}
