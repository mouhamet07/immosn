package sn.immosn.backend.client.web.favoris.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.immosn.backend.client.web.favoris.dto.FavorisResponseDto;
import sn.immosn.backend.client.web.favoris.dto.FavorisStatusDto;
import sn.immosn.backend.favoris.service.FavorisService;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/favoris")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class FavorisController {

    private final FavorisService favorisService;

    /**
     * POST /api/v1/favoris/{annonceId}/toggle
     * Ajoute ou retire l'annonce des favoris (toggle).
     */
    @PostMapping("/{annonceId}/toggle")
    public ResponseEntity<RestResponse<FavorisStatusDto>> toggle(
            @PathVariable Long annonceId, Principal principal) {
        if (annonceId == null || annonceId <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant d'annonce invalide", null));
        }
        FavorisStatusDto result = favorisService.toggle(annonceId, principal.getName());
        return ResponseEntity.ok(RestResponse.success(result, HttpStatus.OK));
    }

    /**
     * GET /api/v1/favoris
     * Liste des favoris du client connecté.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<FavorisResponseDto>> getClientFavoris(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Principal principal) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(
            PagedResponse.fromPage(favorisService.getClientFavoris(principal.getName(), pageable)));
    }

    /**
     * GET /api/v1/favoris/ids
     * Retourne uniquement les IDs d'annonces favorites du client connecté.
     * Utilisé par le store Pinia pour l'état local du bouton ❤️ — sans limite fixe.
     */
    @GetMapping("/ids")
    public ResponseEntity<RestResponse<List<Long>>> getFavorisIds(Principal principal) {
        List<Long> ids = favorisService.getAllFavorisIds(principal.getName());
        return ResponseEntity.ok(RestResponse.success(ids, HttpStatus.OK));
    }

    /**
     * GET /api/v1/favoris/{annonceId}/check
     * Vérifie si une annonce est dans les favoris du client connecté.
     */
    @GetMapping("/{annonceId}/check")
    public ResponseEntity<RestResponse<FavorisStatusDto>> check(
            @PathVariable Long annonceId, Principal principal) {
        if (annonceId == null || annonceId <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant d'annonce invalide", null));
        }
        return ResponseEntity.ok(
            RestResponse.success(favorisService.checkFavoris(annonceId, principal.getName()), HttpStatus.OK));
    }
}
