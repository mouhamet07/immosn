package sn.immosn.backend.client.web.proprietaire.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.immosn.backend.client.web.annonce.dto.AnnonceListDto;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireCreateRequestDto;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireDetailDto;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireResponseDto;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireStatsDto;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireUpdateRequestDto;
import sn.immosn.backend.proprietaire.service.ProprietaireService;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;

@RestController
@RequestMapping("/api/v1/proprietaires")
@RequiredArgsConstructor
@Validated
@Tag(name = "PROPRIETAIRES", description = "Gestion des propriétaires des biens immobiliers et de leurs statistiques")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class ProprietaireController {

    private final ProprietaireService proprietaireService;

    @Operation(
        summary = "Créer un propriétaire",
        description = "Crée un propriétaire pouvant ensuite être associé à un ou plusieurs biens. **Accès : ADMIN ou SUPER_ADMIN**",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<RestResponse<ProprietaireResponseDto>> create(@Valid @RequestBody ProprietaireCreateRequestDto request) {
        ProprietaireResponseDto created = proprietaireService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(RestResponse.success(created, HttpStatus.CREATED));
    }

    @Operation(
        summary = "Modifier un propriétaire",
        description = "Met à jour les informations d'un propriétaire (champs partiels). **Accès : ADMIN ou SUPER_ADMIN**",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{id}")
    public ResponseEntity<RestResponse<ProprietaireResponseDto>> update(
        @PathVariable Long id, @Valid @RequestBody ProprietaireUpdateRequestDto request) {
        ProprietaireResponseDto updated = proprietaireService.update(id, request);
        return ResponseEntity.ok(RestResponse.success(updated, HttpStatus.OK));
    }

    @Operation(
        summary = "Lister les propriétaires",
        description = "Retourne la liste paginée de tous les propriétaires, triée par nom. **Accès : ADMIN ou SUPER_ADMIN**",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<PagedResponse<ProprietaireResponseDto>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "nomComplet"));
        return ResponseEntity.ok(PagedResponse.fromPage(proprietaireService.getAll(pageable)));
    }

    @Operation(
        summary = "Détail d'un propriétaire",
        description = """
            Retourne le profil du propriétaire ainsi que ses statistiques d'activité
            (biens, ventes, locations, annonces actives, visites, contrats, revenus).

            **Accès : ADMIN ou SUPER_ADMIN**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<ProprietaireDetailDto>> getById(@PathVariable Long id) {
        ProprietaireResponseDto proprietaire = proprietaireService.getById(id);
        ProprietaireStatsDto stats = proprietaireService.getStats(id);
        return ResponseEntity.ok(RestResponse.success(new ProprietaireDetailDto(proprietaire, stats), HttpStatus.OK));
    }

    @Operation(
        summary = "Statistiques d'un propriétaire",
        description = """
            Retourne uniquement les statistiques d'activité du propriétaire :
            nombre total de biens, répartition vente/location, annonces actives,
            visites générées, contrats liés et revenus générés.

            **Accès : ADMIN ou SUPER_ADMIN**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{id}/stats")
    public ResponseEntity<RestResponse<ProprietaireStatsDto>> getStats(@PathVariable Long id) {
        return ResponseEntity.ok(RestResponse.success(proprietaireService.getStats(id), HttpStatus.OK));
    }

    @Operation(
        summary = "Biens d'un propriétaire",
        description = "Retourne la liste paginée des biens (annonces) appartenant à ce propriétaire. **Accès : ADMIN ou SUPER_ADMIN**",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{id}/biens")
    public ResponseEntity<PagedResponse<AnnonceListDto>> getBiens(
        @PathVariable Long id,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PagedResponse.fromPage(proprietaireService.getBiens(id, pageable)));
    }
}
