package sn.immosn.backend.client.web.visite.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.immosn.backend.client.web.visite.dto.*;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;
import sn.immosn.backend.visite.service.DemandeVisiteService;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/visites")
@RequiredArgsConstructor
public class DemandeVisiteController {

    private final DemandeVisiteService service;

    /** POST /api/v1/visites — CLIENT : créer une demande */
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RestResponse<DemandeVisiteResponseDto>> create(
            @RequestBody @Valid DemandeVisiteCreateRequestDto request, Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error("Vous devez être connecté", HttpStatus.UNAUTHORIZED));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RestResponse.success(service.create(request, principal.getName()), HttpStatus.CREATED));
    }

    /** GET /api/v1/visites/client — CLIENT : ses demandes */
    @GetMapping("/client")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<PagedResponse<DemandeVisiteResponseDto>> getClientVisites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) StatutDemandeVisite statut,
            Principal principal) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PagedResponse.fromPage(
            service.getClientVisites(principal.getName(), statut, pageable)));
    }

    /** GET /api/v1/visites/admin — ADMIN ou SUPER_ADMIN : toutes les demandes */
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<DemandeVisiteResponseDto>> getAllVisites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) StatutDemandeVisite statut) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PagedResponse.fromPage(service.getAllVisites(statut, pageable)));
    }

    /** PUT /api/v1/visites/{id}/status — changer le statut */
    @PutMapping("/{id}/status")
    public ResponseEntity<RestResponse<DemandeVisiteResponseDto>> updateStatut(
            @PathVariable Long id,
            @RequestBody @Valid UpdateStatutVisiteDto dto,
            Principal principal) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de demande de visite invalide", null));
        }
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error("Vous devez être connecté", HttpStatus.UNAUTHORIZED));
        }
        boolean isAdmin = isAdmin(principal);
        return ResponseEntity.ok(RestResponse.success(
            service.updateStatut(id, dto, principal.getName(), isAdmin), HttpStatus.OK));
    }

    /** PUT /api/v1/visites/{id}/date — ADMIN ou SUPER_ADMIN : modifier la date */
    @PutMapping("/{id}/date")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RestResponse<DemandeVisiteResponseDto>> updateDate(
            @PathVariable Long id,
            @RequestBody @Valid UpdateDateVisiteDto dto) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de demande de visite invalide", null));
        }
        return ResponseEntity.ok(RestResponse.success(service.updateDate(id, dto), HttpStatus.OK));
    }

    /** DELETE /api/v1/visites/{id} — CLIENT : annuler */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RestResponse<Void>> annuler(@PathVariable Long id, Principal principal) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de demande de visite invalide", null));
        }
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error("Vous devez être connecté", HttpStatus.UNAUTHORIZED));
        }
        service.annuler(id, principal.getName());
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
            .body(RestResponse.success(null, HttpStatus.NO_CONTENT));
    }

    private boolean isAdmin(Principal principal) {
        var auth = (org.springframework.security.core.Authentication) principal;
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }
}
