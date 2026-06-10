package sn.immosn.backend.client.web.annonce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.springframework.security.access.prepost.PreAuthorize;
import sn.immosn.backend.annonce.service.AnnonceService;
import sn.immosn.backend.client.web.annonce.dto.AnnonceCreateRequestDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceListDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceResponseDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceUpdateRequestDto;
import sn.immosn.backend.client.web.annonce.dto.SearchAnnonceRequestDto;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;

@RestController
@RequestMapping("/api/v1/annonces")
@RequiredArgsConstructor
@Validated
@Tag(name = "ANNONCES", description = "Publication, consultation, recherche et gestion des annonces immobilières")
public class AnnonceController {

    private final AnnonceService annonceService;

    @Value("${page.default.size}")
    private int defaultPageSize;
    @Value("${page.admin.size}")
    private int adminPageSize;

    @Operation(
        summary = "Créer une annonce",
        description = """
            Publie une nouvelle annonce immobilière (vente ou location).

            L'annonce est immédiatement visible dans la liste publique après création.
            Les coordonnées GPS (latitude/longitude) sont calculées automatiquement
            via le service de géocodage à partir de la localisation fournie.

            **Accès : ADMIN ou SUPER_ADMIN**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Annonce créée et publiée avec succès",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 201,
                      "data": {
                        "id": 15,
                        "libelle": "Villa F5 avec piscine - Almadies",
                        "description": "Magnifique villa de 5 pièces avec piscine privée...",
                        "nbrPieces": 5,
                        "surface": 280.0,
                        "prix": 450000000,
                        "adresse": "Route des Almadies, Villa 12",
                        "region": "Dakar",
                        "departement": "Dakar",
                        "quartier": "Almadies",
                        "latitude": 14.7462,
                        "longitude": -17.5185,
                        "typeBien": {"id": 1, "libelle": "Villa"},
                        "commodites": [{"id": 2, "libelle": "Piscine"}, {"id": 3, "libelle": "Garage"}],
                        "images": ["https://storage.immosn.sn/annonces/15/img1.jpg"],
                        "archived": false,
                        "createdAt": "2024-01-15T09:00:00"
                      }
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Données invalides — champs obligatoires manquants",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping
    public ResponseEntity<RestResponse<AnnonceResponseDto>> create(@RequestBody @Valid AnnonceCreateRequestDto request) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(RestResponse.success(annonceService.createAnnonce(request), HttpStatus.CREATED));
    }

    @Operation(
        summary = "Lister les annonces actives",
        description = """
            Retourne la liste paginée des annonces **actives** (non archivées), triée par défaut
            par date de création décroissante.

            Utilisé par la page d'accueil et la liste publique des annonces.
            Les annonces archivées ne sont jamais incluses dans cette réponse.

            **Accès : PUBLIC — aucun token requis**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des annonces retournée avec succès",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "content": [
                        {
                          "id": 15,
                          "libelle": "Villa F5 avec piscine - Almadies",
                          "prix": 450000000,
                          "departement": "Dakar",
                          "quartier": "Almadies",
                          "typeBien": "Villa",
                          "nbrPieces": 5,
                          "surface": 280.0,
                          "imagePrincipale": "https://storage.immosn.sn/annonces/15/img1.jpg",
                          "createdAt": "2024-01-15T09:00:00"
                        }
                      ],
                      "page": 0, "size": 12, "totalElements": 48, "totalPages": 4, "last": false
                    }""")))
    })
    @GetMapping
    public ResponseEntity<PagedResponse<AnnonceListDto>> getAll(
        @Parameter(description = "Numéro de page (commence à 0)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Nombre d'éléments par page (défaut : valeur configurée)", example = "12")
        @RequestParam(required = false) Integer size,
        @Parameter(description = "Champ de tri", example = "createdAt")
        @RequestParam(defaultValue = "createdAt") String sort,
        @Parameter(description = "Sens du tri : ASC ou DESC", example = "DESC")
        @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        int resolvedSize = (size != null && size > 0) ? size : defaultPageSize;
        Pageable pageable = PageRequest.of(page, resolvedSize, Sort.by(direction, sort));
        Page<AnnonceListDto> annonceListDto = annonceService.getAllAnnonces(pageable);
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(PagedResponse.fromPage(annonceListDto));
    }

    @Operation(
        summary = "Détail d'une annonce",
        description = """
            Retourne toutes les informations d'une annonce spécifique : description complète,
            commodités, galerie d'images, type de bien, coordonnées GPS et dates.

            **Accès : PUBLIC — aucun token requis**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Détail de l'annonce retourné avec succès",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide (null ou ≤ 0)",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Annonce non trouvée",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {"success":false,"status":404,"message":"Annonce non trouvée","data":null}""")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<AnnonceResponseDto>> getById(
            @Parameter(description = "Identifiant de l'annonce", required = true, example = "15")
            @PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant d'annonce invalide", null));
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(annonceService.getAnnonceById(id), HttpStatus.OK));
    }

    @Operation(
        summary = "Modifier une annonce",
        description = """
            Met à jour complètement une annonce existante. Tous les champs fournis remplacent
            les valeurs actuelles.

            Les coordonnées GPS sont recalculées si la localisation change.

            **Accès : ADMIN ou SUPER_ADMIN**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Annonce modifiée avec succès",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide ou données incorrectes",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Annonce non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}")
    public ResponseEntity<RestResponse<AnnonceResponseDto>> update(
        @Parameter(description = "Identifiant de l'annonce à modifier", required = true, example = "15")
        @PathVariable Long id,
        @RequestBody @Valid AnnonceUpdateRequestDto request) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant d'annonce invalide", null));
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(annonceService.updateAnnonce(id, request), HttpStatus.OK));
    }

    @Operation(
        summary = "Archiver une annonce (soft delete)",
        description = """
            Archive une annonce : elle disparaît de la liste publique mais les données
            sont conservées (liens contrats, favoris, visites restent intacts).

            L'annonce peut être restaurée ultérieurement via `PATCH /{id}/restore`.

            **Accès : ADMIN ou SUPER_ADMIN**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Annonce archivée avec succès — aucun contenu retourné"),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Annonce non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<RestResponse<Void>> archive(
            @Parameter(description = "Identifiant de l'annonce à archiver", required = true, example = "15")
            @PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant d'annonce invalide", null));
        }
        annonceService.archiveAnnonce(id);
        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .body(RestResponse.success(null, HttpStatus.NO_CONTENT));
    }

    @Operation(
        summary = "Restaurer une annonce archivée",
        description = """
            Réactive une annonce archivée. Elle redevient visible dans la liste publique.

            **Accès : ADMIN ou SUPER_ADMIN**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Annonce restaurée avec succès — aucun contenu retourné"),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Annonce non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/{id}/restore")
    public ResponseEntity<RestResponse<Void>> restore(
            @Parameter(description = "Identifiant de l'annonce à restaurer", required = true, example = "15")
            @PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant d'annonce invalide", null));
        }
        annonceService.restoreAnnonce(id);
        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .body(RestResponse.success(null, HttpStatus.NO_CONTENT));
    }

    @Operation(
        summary = "Recherche multicritère d'annonces",
        description = """
            Recherche avancée avec filtres combinables. Tous les champs du corps sont **optionnels** :
            seuls les champs renseignés sont pris en compte dans le filtre.

            Exemples de combinaisons :
            - Type villa + prix max 300M FCFA
            - Appartement à Dakar avec climatisation et parking
            - Terrain > 500m² dans le département de Thiès

            **Accès : PUBLIC — aucun token requis**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Résultats de recherche retournés (liste vide si aucun résultat)",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "Exemple recherche villa Dakar", value = """
                    {
                      "content": [
                        {
                          "id": 15,
                          "libelle": "Villa F5 avec piscine - Almadies",
                          "prix": 450000000,
                          "departement": "Dakar",
                          "quartier": "Almadies",
                          "typeBien": "Villa",
                          "nbrPieces": 5,
                          "surface": 280.0
                        }
                      ],
                      "page": 0, "size": 12, "totalElements": 3, "totalPages": 1, "last": true
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Paramètres de recherche invalides (ex: prixMin > prixMax)",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/search")
    public ResponseEntity<PagedResponse<AnnonceListDto>> search(
            @RequestBody @Valid SearchAnnonceRequestDto request) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(PagedResponse.fromPage(annonceService.searchAnnonces(request)));
    }

    @Operation(
        summary = "Détail d'une annonce (vue admin)",
        description = """
            Retourne les informations complètes d'une annonce, **qu'elle soit archivée ou non**.

            Contrairement à `GET /api/v1/annonces/{id}` (vue publique), cet endpoint ignore
            le statut d'archivage pour permettre à l'administrateur d'inspecter et modifier
            toute annonce, y compris les annonces archivées.

            **Accès : ADMIN ou SUPER_ADMIN**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Détail de l'annonce retourné avec succès",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Annonce non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RestResponse<AnnonceResponseDto>> getByIdAdmin(
            @Parameter(description = "Identifiant de l'annonce", required = true, example = "15")
            @PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant d'annonce invalide", null));
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(annonceService.getAnnonceByIdAdmin(id), HttpStatus.OK));
    }

    @Operation(
        summary = "Lister toutes les annonces (vue admin)",
        description = """
            Retourne la liste paginée de **toutes** les annonces — actives et archivées.

            Contrairement à `GET /api/v1/annonces` (vue publique), cet endpoint inclut
            les annonces archivées pour permettre leur gestion et restauration.

            **Accès : ADMIN ou SUPER_ADMIN**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste complète des annonces (actives + archivées)",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/admin")
    public ResponseEntity<PagedResponse<AnnonceListDto>> getAllAdmin(
        @Parameter(description = "Numéro de page (commence à 0)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Nombre d'éléments par page (défaut : taille admin configurée)", example = "20")
        @RequestParam(required = false) Integer size,
        @Parameter(description = "Champ de tri", example = "createdAt")
        @RequestParam(defaultValue = "createdAt") String sort,
        @Parameter(description = "Sens du tri : ASC ou DESC", example = "DESC")
        @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        int resolvedSize = (size != null && size > 0) ? size : adminPageSize;
        Pageable pageable = PageRequest.of(page, resolvedSize, Sort.by(direction, sort));
        Page<AnnonceListDto> annonceListDto = annonceService.getAllAnnoncesAdmin(pageable);
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(PagedResponse.fromPage(annonceListDto));
    }
}
