package sn.immosn.backend.client.web.annonce.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@CrossOrigin(origins = "http://localhost:5173")
public class AnnonceController {

    private final AnnonceService annonceService;

    @Value("${page.default.size}")
    private int defaultPageSize;
    @Value("${page.admin.size}")
    private int adminPageSize;

    /**
     * POST /api/v1/annonces
     * Crée et publie une nouvelle annonce immobilière
     * Retour: HTTP 201 Created
     */
    @PostMapping
    public ResponseEntity<RestResponse<AnnonceResponseDto>> create(@RequestBody @Valid AnnonceCreateRequestDto request) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(RestResponse.success(annonceService.createAnnonce(request), HttpStatus.CREATED));
    }

    /**
     * GET /api/v1/annonces
     * Récupère la liste paginée des annonces ACTIVES (visibles pour les clients)
     * Exclut les annonces archivées
     * Pagination configurable par queryparams
     */
    @GetMapping
    public ResponseEntity<PagedResponse<AnnonceListDto>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) Integer size,
        @RequestParam(defaultValue = "createdAt") String sort,
        @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        int resolvedSize = (size != null && size > 0) ? size : defaultPageSize;
        Pageable pageable = PageRequest.of(page, resolvedSize, Sort.by(direction, sort));
        Page<AnnonceListDto> annonceListDto = annonceService.getAllAnnonces(pageable);
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(PagedResponse.fromPage(annonceListDto));
    }

    /**
     * GET /api/v1/annonces/{id}
     * Récupère les détails complets d'une annonce spécifique
     * Inclut: description, commodités, images, typeBien, dates
     */
    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<AnnonceResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(annonceService.getAnnonceById(id), HttpStatus.OK));
    }

    /**
     * PUT /api/v1/annonces/{id}
     * Modifie complètement une annonce existante
     * Remplace tous les champs fournis
     */
    @PutMapping("/{id}")
    public ResponseEntity<RestResponse<AnnonceResponseDto>> update(
        @PathVariable Long id,
        @RequestBody @Valid AnnonceUpdateRequestDto request) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(annonceService.updateAnnonce(id, request), HttpStatus.OK));
    }

    /**
     * DELETE /api/v1/annonces/{id}
     * Archive une annonce (soft delete): marque comme inactive sans supprimer les données
     * L'annonce n'est plus visible pour les clients mais conserve son historique
     * Retour: HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<RestResponse<Void>> archive(@PathVariable Long id) {
        annonceService.archiveAnnonce(id);
        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .body(RestResponse.success(null, HttpStatus.NO_CONTENT));
    }

    /**
     * PATCH /api/v1/annonces/{id}/restore
     * Restaure une annonce archivée: réactive l'annonce pour la rendre visible
     * Retour: HTTP 204 No Content
     */
    @PatchMapping("/{id}/restore")
    public ResponseEntity<RestResponse<Void>> restore(@PathVariable Long id) {
        annonceService.restoreAnnonce(id);
        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .body(RestResponse.success(null, HttpStatus.NO_CONTENT));
    }

    /**
     * POST /api/v1/annonces/search
     * Recherche avancée avec filtres combinables (Sprint 2 – PB05/PB08)
     * Body: SearchAnnonceRequestDto — tous les champs sont optionnels
     */
    @PostMapping("/search")
    public ResponseEntity<PagedResponse<AnnonceListDto>> search(
            @RequestBody @Valid SearchAnnonceRequestDto request) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(PagedResponse.fromPage(annonceService.searchAnnonces(request)));
    }

    /**
     * GET /api/v1/annonces/admin
     * Récupère la liste paginée de TOUTES les annonces (y compris archivées)
     * Vue réservée aux administrateurs pour gestion complète
     * Pagination configurable par queryparams
     */
    @GetMapping("/admin")
    public ResponseEntity<PagedResponse<AnnonceListDto>> getAllAdmin(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) Integer size,
        @RequestParam(defaultValue = "createdAt") String sort,
        @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        int resolvedSize = (size != null && size > 0) ? size : adminPageSize;
        Pageable pageable = PageRequest.of(page, resolvedSize, Sort.by(direction, sort));
        Page<AnnonceListDto> annonceListDto = annonceService.getAllAnnoncesAdmin(pageable);
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(PagedResponse.fromPage(annonceListDto));
    }
}
