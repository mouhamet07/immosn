package sn.immosn.backend.client.web.annonce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import sn.immosn.backend.annonce.service.CommoditeService;
import sn.immosn.backend.client.web.annonce.dto.CommoditeRequestDto;
import sn.immosn.backend.client.web.annonce.dto.CommoditeResponseDto;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;

@RestController
@RequestMapping("/api/v1/commodites")
@RequiredArgsConstructor
@Validated
@Tag(name = "COMMODITES", description = "Gestion du référentiel des commodités disponibles pour les annonces (piscine, garage, climatisation, etc.)")
public class CommoditeController {

    private final CommoditeService commoditeService;

    @Operation(
        summary = "Créer une commodité",
        description = """
            Ajoute une nouvelle commodité au référentiel de la plateforme.

            Les commodités créées sont ensuite disponibles pour être associées aux annonces.
            Exemples : Piscine, Garage, Climatisation, Jardin, Ascenseur, Gardiennage.

            Le libellé doit être unique.

            **Accès : ADMIN ou SUPER_ADMIN**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Commodité créée avec succès",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 201,
                      "data": {"id": 5, "libelle": "Piscine", "isArchived": false}
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Libellé manquant ou vide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "409", description = "Une commodité avec ce libellé existe déjà",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping
    public ResponseEntity<RestResponse<CommoditeResponseDto>> create(
        @RequestBody @Valid CommoditeRequestDto req)
    {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(RestResponse.success(commoditeService.createCommodite(req), HttpStatus.CREATED));
    }

    @Operation(
        summary = "Lister toutes les commodités actives",
        description = """
            Retourne la liste complète (non paginée) de toutes les commodités **actives**.

            Utilisé pour alimenter les formulaires de création/modification d'annonces
            et les filtres de recherche multicritère.

            **Accès : PUBLIC — aucun token requis**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des commodités actives",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 200,
                      "data": [
                        {"id": 1, "libelle": "Climatisation", "isArchived": false},
                        {"id": 2, "libelle": "Garage",        "isArchived": false},
                        {"id": 3, "libelle": "Piscine",       "isArchived": false},
                        {"id": 4, "libelle": "Gardiennage",   "isArchived": false}
                      ]
                    }""")))
    })
    @GetMapping
    public ResponseEntity<RestResponse<List<CommoditeResponseDto>>> getAll() {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(commoditeService.getAllCommodites(), HttpStatus.OK));
    }

    @Operation(
        summary = "Lister toutes les commodités paginées (vue admin)",
        description = """
            Retourne la liste paginée de **toutes** les commodités — actives et archivées.

            Utilisé par l'interface d'administration pour gérer le référentiel complet.

            **Accès : PUBLIC (mais généralement appelé par les admins)**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste paginée des commodités (actives + archivées)",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/paged")
    public ResponseEntity<PagedResponse<CommoditeResponseDto>> getAllPaged(
        @Parameter(description = "Numéro de page (commence à 0)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Nombre d'éléments par page", example = "10")
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CommoditeResponseDto> result = commoditeService.getAllCommoditesPaged(pageable);
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(PagedResponse.fromPage(result));
    }

    @Operation(
        summary = "Détail d'une commodité",
        description = """
            Retourne les informations d'une commodité spécifique par son identifiant.

            **Accès : PUBLIC — aucun token requis**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Commodité trouvée",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {"success":true,"status":200,"data":{"id":3,"libelle":"Piscine","isArchived":false}}"""))),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide (null ou ≤ 0)",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Commodité non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<CommoditeResponseDto>> getById(
            @Parameter(description = "Identifiant de la commodité", required = true, example = "3")
            @PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de commodité invalide", null));
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(commoditeService.getCommoditeById(id), HttpStatus.OK));
    }

    @Operation(
        summary = "Modifier une commodité",
        description = """
            Met à jour le libellé d'une commodité existante.

            Le nouveau libellé doit être unique dans le référentiel.

            **Accès : ADMIN ou SUPER_ADMIN**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Commodité modifiée avec succès",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide ou libellé vide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Commodité non trouvée",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "409", description = "Ce libellé est déjà utilisé par une autre commodité",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}")
    public ResponseEntity<RestResponse<CommoditeResponseDto>> update(
        @Parameter(description = "Identifiant de la commodité à modifier", required = true, example = "3")
        @PathVariable Long id,
        @RequestBody @Valid CommoditeRequestDto req) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de commodité invalide", null));
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(commoditeService.updateCommodite(id, req), HttpStatus.OK));
    }

    @Operation(
        summary = "Archiver une commodité (soft delete)",
        description = """
            Archive une commodité : elle n'apparaît plus dans la liste publique
            mais les annonces qui l'utilisaient déjà la conservent.

            La commodité peut être restaurée via `PATCH /{id}/restore`.

            **Accès : ADMIN ou SUPER_ADMIN**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Commodité archivée — aucun contenu retourné"),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Commodité non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<RestResponse<Void>> archive(
            @Parameter(description = "Identifiant de la commodité à archiver", required = true, example = "3")
            @PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de commodité invalide", null));
        }
        commoditeService.archiveCommodite(id);
        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .body(RestResponse.success(null, HttpStatus.NO_CONTENT));
    }

    @Operation(
        summary = "Restaurer une commodité archivée",
        description = """
            Réactive une commodité archivée. Elle redevient disponible pour les nouvelles annonces.

            **Accès : ADMIN ou SUPER_ADMIN**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Commodité restaurée avec succès",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Commodité non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/{id}/restore")
    public ResponseEntity<RestResponse<CommoditeResponseDto>> restore(
            @Parameter(description = "Identifiant de la commodité à restaurer", required = true, example = "3")
            @PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de commodité invalide", null));
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(commoditeService.restoreCommodite(id), HttpStatus.OK));
    }
}
