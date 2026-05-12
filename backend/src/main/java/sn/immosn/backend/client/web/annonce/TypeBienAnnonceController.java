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
import sn.immosn.backend.annonce.service.TypeBienAnnonceService;
import sn.immosn.backend.client.web.annonce.dto.TypeBienRequestDto;
import sn.immosn.backend.client.web.annonce.dto.TypeBienResponseDto;

@RestController
@RequestMapping("/api/v1/types-bien")
@RequiredArgsConstructor
@Validated
public class TypeBienAnnonceController {

    private final TypeBienAnnonceService typeBienService;

    /**
     * POST /api/v1/types-bien
     * Crée un nouveau type de bien (villa, appartement, terrain, etc.)
     */
    @PostMapping
    public TypeBienResponseDto create(@RequestBody @Valid TypeBienRequestDto req) {
        return typeBienService.createTypeBien(req);
    }

    /**
     * GET /api/v1/types-bien
     * Récupère la liste de tous les types de bien actifs
     */
    @GetMapping
    public List<TypeBienResponseDto> getAll() {
        return typeBienService.getAllTypesBien();
    }

    /**
     * GET /api/v1/types-bien/{id}
     * Récupère les détails d'un type de bien spécifique
     */
    @GetMapping("/{id}")
    public TypeBienResponseDto getById(@PathVariable Long id) {
        return typeBienService.getTypeBienById(id);
    }

    /**
     * PUT /api/v1/types-bien/{id}
     * Modifie complètement un type de bien existant
     */
    @PutMapping("/{id}")
    public TypeBienResponseDto update(
        @PathVariable Long id,
        @RequestBody @Valid TypeBienRequestDto req) {
        return typeBienService.updateTypeBien(id, req);
    }

    /**
     * DELETE /api/v1/types-bien/{id}
     * Archive (soft delete) un type de bien sans supprimer les données
     */
    @DeleteMapping("/{id}")
    public void archive(@PathVariable Long id) {
        typeBienService.archiveTypeBien(id);
    }
}
