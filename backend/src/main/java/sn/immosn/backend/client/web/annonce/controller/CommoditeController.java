package sn.immosn.backend.client.web.annonce.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/commodites")
@RequiredArgsConstructor
@Validated
public class CommoditeController {

    private final CommoditeService commoditeService;

    /**
     * POST /api/v1/commodites
     * Crée une nouvelle commodité (piscine, garage, jardin, climatisation, etc.)
     */
    @PostMapping
    public ResponseEntity<RestResponse<CommoditeResponseDto>> create(
        @RequestBody @Valid CommoditeRequestDto req) 
    {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(RestResponse.success(commoditeService.createCommodite(req), HttpStatus.CREATED));
    }

    /**
     * GET /api/v1/commodites
     * Récupère la liste de toutes les commodités actives disponibles
     */
    @GetMapping
    public ResponseEntity<RestResponse<List<CommoditeResponseDto>>> getAll() {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(commoditeService.getAllCommodites(), HttpStatus.OK));
    }

    /**
     * GET /api/v1/commodites
     * Récupère la liste de toutes les commodités pagines actif comme inactives disponibles
     */
    @GetMapping("/paged")
    public ResponseEntity<PagedResponse<CommoditeResponseDto>> getAllPaged(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CommoditeResponseDto> result = commoditeService.getAllCommoditesPaged(pageable);
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(PagedResponse.fromPage(result));
    }

    /**
     * GET /api/v1/commodites/{id}
     * Récupère les détails d'une commodité spécifique
     */
    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<CommoditeResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(commoditeService.getCommoditeById(id), HttpStatus.OK));
    }

    /**
     * PUT /api/v1/commodites/{id}
     * Modifie complètement une commodité existante
     */
    @PutMapping("/{id}")
    public ResponseEntity<RestResponse<CommoditeResponseDto>> update(
        @PathVariable Long id,
        @RequestBody @Valid CommoditeRequestDto req) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(commoditeService.updateCommodite(id, req), HttpStatus.OK));
    }

    /**
     * DELETE /api/v1/commodites/{id}
     * Archive (soft delete) une commodité sans supprimer les données
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<RestResponse<Void>> archive(@PathVariable Long id) {
        commoditeService.archiveCommodite(id);
        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .body(RestResponse.success(null, HttpStatus.NO_CONTENT));
    }
}
