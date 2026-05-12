package sn.immosn.backend.client.web.annonce;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import sn.immosn.backend.annonce.service.CommoditeService;
import sn.immosn.backend.client.web.annonce.dto.CommoditeRequestDto;
import sn.immosn.backend.client.web.annonce.dto.CommoditeResponseDto;

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
    public CommoditeResponseDto create(@RequestBody @Valid CommoditeRequestDto req) {
        return commoditeService.createCommodite(req);
    }

    /**
     * GET /api/v1/commodites
     * Récupère la liste de toutes les commodités actives disponibles
     */
    @GetMapping
    public List<CommoditeResponseDto> getAll() {
        return commoditeService.getAllCommodites();
    }

    /**
     * GET /api/v1/commodites/{id}
     * Récupère les détails d'une commodité spécifique
     */
    @GetMapping("/{id}")
    public CommoditeResponseDto getById(@PathVariable Long id) {
        return commoditeService.getCommoditeById(id);
    }

    /**
     * PUT /api/v1/commodites/{id}
     * Modifie complètement une commodité existante
     */
    @PutMapping("/{id}")
    public CommoditeResponseDto update(
        @PathVariable Long id,
        @RequestBody @Valid CommoditeRequestDto req) {
        return commoditeService.updateCommodite(id, req);
    }

    /**
     * DELETE /api/v1/commodites/{id}
     * Archive (soft delete) une commodité sans supprimer les données
     */
    @DeleteMapping("/{id}")
    public void archive(@PathVariable Long id) {
        commoditeService.archiveCommodite(id);
    }
}
