package sn.immosn.backend.client.web.annonce;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import sn.immosn.backend.annonce.service.AnnonceService;
import sn.immosn.backend.client.web.annonce.dto.AnnonceCreateRequestDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceListDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceResponseDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceUpdateRequestDto;

@RestController
@RequestMapping("/api/v1/annonces")
@RequiredArgsConstructor
@Validated
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
    public AnnonceResponseDto create(@RequestBody @Valid AnnonceCreateRequestDto request) {
        return annonceService.createAnnonce(request);
    }

    /**
     * GET /api/v1/annonces
     * Récupère la liste paginée des annonces ACTIVES (visibles pour les clients)
     * Exclut les annonces archivées
     * Pagination configurable par queryparams
     */
    @GetMapping
    public Page<AnnonceListDto> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) Integer size,
        @RequestParam(defaultValue = "createdAt") String sort,
        @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        int resolvedSize = size != null ? size : defaultPageSize;
        return annonceService.getAllAnnonces(PageRequest.of(page, resolvedSize, Sort.by(direction, sort)));
    }

    /**
     * GET /api/v1/annonces/{id}
     * Récupère les détails complets d'une annonce spécifique
     * Inclut: description, commodités, images, typeBien, dates
     */
    @GetMapping("/{id}")
    public AnnonceResponseDto getById(@PathVariable Long id) {
        return annonceService.getAnnonceById(id);
    }

    /**
     * PUT /api/v1/annonces/{id}
     * Modifie complètement une annonce existante
     * Remplace tous les champs fournis
     */
    @PutMapping("/{id}")
    public AnnonceResponseDto update(
        @PathVariable Long id,
        @RequestBody @Valid AnnonceUpdateRequestDto request) {
        return annonceService.updateAnnonce(id, request);
    }

    /**
     * DELETE /api/v1/annonces/{id}
     * Archive une annonce (soft delete): marque comme inactive sans supprimer les données
     * L'annonce n'est plus visible pour les clients mais conserve son historique
     * Retour: HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public void archive(@PathVariable Long id) {
        annonceService.archiveAnnonce(id);
    }

    /**
     * PATCH /api/v1/annonces/{id}/restore
     * Restaure une annonce archivée: réactive l'annonce pour la rendre visible
     * Retour: HTTP 204 No Content
     */
    @PatchMapping("/{id}/restore")
    public void restore(@PathVariable Long id) {
        annonceService.restoreAnnonce(id);
    }

    /**
     * GET /api/v1/annonces/admin
     * Récupère la liste paginée de TOUTES les annonces (y compris archivées)
     * Vue réservée aux administrateurs pour gestion complète
     * Pagination configurable par queryparams
     */
    @GetMapping("/admin")
    public Page<AnnonceListDto> getAllAdmin(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) Integer size,
        @RequestParam(defaultValue = "createdAt") String sort,
        @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        int resolvedSize = size != null ? size : adminPageSize;
        return annonceService.getAllAnnoncesAdmin(PageRequest.of(page, resolvedSize, Sort.by(direction, sort)));
    }
}
