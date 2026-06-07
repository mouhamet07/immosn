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
import sn.immosn.backend.annonce.service.TypeBienAnnonceService;
import sn.immosn.backend.client.web.annonce.dto.TypeBienRequestDto;
import sn.immosn.backend.client.web.annonce.dto.TypeBienResponseDto;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;

@RestController
@RequestMapping("/api/v1/types-bien")
@RequiredArgsConstructor
@Validated
@Tag(name = "TYPES DE BIENS", description = "Gestion du référentiel des types de biens immobiliers (Villa, Appartement, Terrain, Bureau, etc.)")
public class TypeBienAnnonceController {

    private final TypeBienAnnonceService typeBienService;

    @Operation(
        summary = "Créer un type de bien",
        description = """
            Ajoute un nouveau type de bien immobilier au référentiel de la plateforme.

            Les types créés sont ensuite disponibles pour catégoriser les annonces
            et pour les filtres de recherche.
            Exemples : Villa, Appartement, Studio, Terrain, Immeuble, Bureau, Entrepôt.

            Le libellé doit être unique.

            **Accès : ADMIN ou SUPER_ADMIN**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Type de bien créé avec succès",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 201,
                      "data": {"id": 6, "libelle": "Studio", "isArchived": false}
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Libellé manquant ou vide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "409", description = "Un type de bien avec ce libellé existe déjà",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping
    public ResponseEntity<RestResponse<TypeBienResponseDto>> create(
        @RequestBody @Valid TypeBienRequestDto req)
    {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(RestResponse.success(typeBienService.createTypeBien(req), HttpStatus.CREATED));
    }

    @Operation(
        summary = "Lister tous les types de biens actifs",
        description = """
            Retourne la liste complète (non paginée) de tous les types de biens **actifs**.

            Utilisé pour alimenter les sélecteurs dans les formulaires d'annonces
            et dans les filtres de recherche multicritère.

            **Accès : PUBLIC — aucun token requis**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des types de biens actifs",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 200,
                      "data": [
                        {"id": 1, "libelle": "Villa",        "isArchived": false},
                        {"id": 2, "libelle": "Appartement",  "isArchived": false},
                        {"id": 3, "libelle": "Terrain",      "isArchived": false},
                        {"id": 4, "libelle": "Bureau",       "isArchived": false},
                        {"id": 5, "libelle": "Studio",       "isArchived": false}
                      ]
                    }""")))
    })
    @GetMapping
    public ResponseEntity<RestResponse<List<TypeBienResponseDto>>> getAll() {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(typeBienService.getAllTypesBien(), HttpStatus.OK));
    }

    @Operation(
        summary = "Lister tous les types de biens paginés (vue admin)",
        description = """
            Retourne la liste paginée de **tous** les types de biens — actifs et archivés.

            Utilisé par l'interface d'administration pour gérer le référentiel complet.

            **Accès : PUBLIC (mais généralement appelé par les admins)**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste paginée des types de biens (actifs + archivés)",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/paged")
    public ResponseEntity<PagedResponse<TypeBienResponseDto>> getAllPaged(
        @Parameter(description = "Numéro de page (commence à 0)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Nombre d'éléments par page", example = "10")
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TypeBienResponseDto> result = typeBienService.getAllTypesBienPaged(pageable);
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(PagedResponse.fromPage(result));
    }

    @Operation(
        summary = "Détail d'un type de bien",
        description = """
            Retourne les informations d'un type de bien spécifique par son identifiant.

            **Accès : PUBLIC — aucun token requis**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Type de bien trouvé",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {"success":true,"status":200,"data":{"id":1,"libelle":"Villa","isArchived":false}}"""))),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide (null ou ≤ 0)",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Type de bien non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<TypeBienResponseDto>> getById(
            @Parameter(description = "Identifiant du type de bien", required = true, example = "1")
            @PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de type de bien invalide", null));
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(typeBienService.getTypeBienById(id), HttpStatus.OK));
    }

    @Operation(
        summary = "Modifier un type de bien",
        description = """
            Met à jour le libellé d'un type de bien existant.

            Le nouveau libellé doit être unique dans le référentiel.

            **Accès : ADMIN ou SUPER_ADMIN**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Type de bien modifié avec succès",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide ou libellé vide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Type de bien non trouvé",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "409", description = "Ce libellé est déjà utilisé par un autre type de bien",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}")
    public ResponseEntity<RestResponse<TypeBienResponseDto>> update(
        @Parameter(description = "Identifiant du type de bien à modifier", required = true, example = "1")
        @PathVariable Long id,
        @RequestBody @Valid TypeBienRequestDto req) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de type de bien invalide", null));
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(typeBienService.updateTypeBien(id, req), HttpStatus.OK));
    }

    @Operation(
        summary = "Archiver un type de bien (soft delete)",
        description = """
            Archive un type de bien : il n'apparaît plus dans la liste publique
            mais les annonces qui l'utilisaient déjà le conservent.

            Le type peut être restauré via `PATCH /{id}/restore`.

            **Accès : ADMIN ou SUPER_ADMIN**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Type de bien archivé — aucun contenu retourné"),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Type de bien non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<RestResponse<Void>> archive(
            @Parameter(description = "Identifiant du type de bien à archiver", required = true, example = "1")
            @PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de type de bien invalide", null));
        }
        typeBienService.archiveTypeBien(id);
        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .body(RestResponse.success(null, HttpStatus.NO_CONTENT));
    }

    @Operation(
        summary = "Restaurer un type de bien archivé",
        description = """
            Réactive un type de bien archivé. Il redevient disponible pour les nouvelles annonces.

            **Accès : ADMIN ou SUPER_ADMIN**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Type de bien restauré avec succès",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Type de bien non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/{id}/restore")
    public ResponseEntity<RestResponse<TypeBienResponseDto>> restore(
            @Parameter(description = "Identifiant du type de bien à restaurer", required = true, example = "1")
            @PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de type de bien invalide", null));
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(typeBienService.restoreTypeBien(id), HttpStatus.OK));
    }
}
