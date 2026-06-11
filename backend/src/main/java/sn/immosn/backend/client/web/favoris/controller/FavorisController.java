package sn.immosn.backend.client.web.favoris.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "FAVORIS", description = "Gestion des annonces favorites du client connecté — ajout, suppression, consultation et vérification")
@SecurityRequirement(name = "bearerAuth")
public class FavorisController {

    private final FavorisService favorisService;

    @Operation(
        summary = "Ajouter/Retirer un favori (toggle)",
        description = """
            Bascule l'état favori d'une annonce pour le client connecté :

            - Si l'annonce **n'est pas** dans les favoris → elle est **ajoutée** (`isFavoris: true`)
            - Si l'annonce **est déjà** dans les favoris → elle est **retirée** (`isFavoris: false`)

            Cette logique évite d'avoir des endpoints séparés add/remove.

            **Accès : CLIENT uniquement**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statut du favori mis à jour",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "Ajouté aux favoris", value = """
                    {
                      "success": true, "status": 200,
                      "data": {"annonceId": 15, "isFavoris": true}
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Identifiant d'annonce invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle CLIENT requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Annonce non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/{annonceId}/toggle")
    public ResponseEntity<RestResponse<FavorisStatusDto>> toggle(
            @Parameter(description = "Identifiant de l'annonce à ajouter/retirer des favoris", required = true, example = "15")
            @PathVariable Long annonceId, Principal principal) {
        if (annonceId == null || annonceId <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant d'annonce invalide", null));
        }
        FavorisStatusDto result = favorisService.toggle(annonceId, principal.getName());
        return ResponseEntity.ok(RestResponse.success(result, HttpStatus.OK));
    }

    @Operation(
        summary = "Mes favoris",
        description = """
            Retourne la liste paginée des annonces favorites du client connecté,
            triée par date d'ajout décroissante.

            Chaque favori inclut le résumé de l'annonce (libellé, prix, adresse, image).

            Les annonces archivées sont automatiquement exclues de cette liste.

            **Accès : CLIENT uniquement**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste paginée des favoris (annonces archivées exclues)",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "data": [
                        {
                          "annonceId": 15,
                          "libelle": "Villa F5 - Almadies",
                          "prix": 450000000,
                          "adresse": "Route des Almadies",
                          "typeBien": "Villa",
                          "nbrPieces": 5,
                          "surface": 280.0,
                          "imagePrincipale": "https://storage.immosn.sn/annonces/15/img1.jpg",
                          "addedAt": "2024-01-15T12:00:00"
                        }
                      ],
                      "totalElements": 5, "totalPages": 1, "currentPage": 0, "pageSize": 12, "isFirst": true, "isLast": true
                    }"""))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle CLIENT requis",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping
    public ResponseEntity<PagedResponse<FavorisResponseDto>> getClientFavoris(
            @Parameter(description = "Numéro de page", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page", example = "12") @RequestParam(defaultValue = "12") int size,
            Principal principal) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(
            PagedResponse.fromPage(favorisService.getClientFavoris(principal.getName(), pageable)));
    }

    @Operation(
        summary = "IDs de mes favoris",
        description = """
            Retourne la liste de **tous** les identifiants d'annonces favorites du client connecté,
            sans pagination.

            Utilisé par le store Pinia du frontend pour maintenir l'état local du bouton ❤️ :
            à l'initialisation, le frontend charge ces IDs et les stocke en mémoire pour
            afficher instantanément le statut favori sur chaque carte d'annonce.

            Les annonces archivées sont automatiquement exclues : un client ne verra jamais
            le cœur actif sur une annonce archivée.

            **Accès : CLIENT uniquement**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste de tous les IDs d'annonces favorites",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 200,
                      "data": [15, 23, 47, 52]
                    }"""))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle CLIENT requis",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/ids")
    public ResponseEntity<RestResponse<List<Long>>> getFavorisIds(Principal principal) {
        List<Long> ids = favorisService.getAllFavorisIds(principal.getName());
        return ResponseEntity.ok(RestResponse.success(ids, HttpStatus.OK));
    }

    @Operation(
        summary = "Vérifier si une annonce est en favoris",
        description = """
            Vérifie si une annonce spécifique est dans les favoris du client connecté.

            Utilisé pour afficher l'état du bouton ❤️ sur la page de détail d'une annonce.

            **Accès : CLIENT uniquement**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statut favori de l'annonce",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {"success":true,"status":200,"data":{"annonceId":15,"isFavoris":true}}"""))),
        @ApiResponse(responseCode = "400", description = "Identifiant d'annonce invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle CLIENT requis",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{annonceId}/check")
    public ResponseEntity<RestResponse<FavorisStatusDto>> check(
            @Parameter(description = "Identifiant de l'annonce à vérifier", required = true, example = "15")
            @PathVariable Long annonceId, Principal principal) {
        if (annonceId == null || annonceId <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant d'annonce invalide", null));
        }
        return ResponseEntity.ok(
            RestResponse.success(favorisService.checkFavoris(annonceId, principal.getName()), HttpStatus.OK));
    }
}
