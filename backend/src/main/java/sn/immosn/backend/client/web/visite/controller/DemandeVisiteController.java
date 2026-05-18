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
@CrossOrigin(origins = "http://localhost:5173")
public class DemandeVisiteController {

    private final DemandeVisiteService service;

    /** POST /api/v1/visites — CLIENT : créer une demande */
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RestResponse<DemandeVisiteResponseDto>> create(
            @RequestBody @Valid DemandeVisiteCreateRequestDto request, Principal principal) {
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

    /** GET /api/v1/visites/admin — ADMIN : toutes les demandes */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
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
        boolean isAdmin = isAdmin(principal);
        return ResponseEntity.ok(RestResponse.success(
            service.updateStatut(id, dto, principal.getName(), isAdmin), HttpStatus.OK));
    }

    /** PUT /api/v1/visites/{id}/date — ADMIN : modifier la date */
    @PutMapping("/{id}/date")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<DemandeVisiteResponseDto>> updateDate(
            @PathVariable Long id,
            @RequestBody @Valid UpdateDateVisiteDto dto) {
        return ResponseEntity.ok(RestResponse.success(service.updateDate(id, dto), HttpStatus.OK));
    }

    /** DELETE /api/v1/visites/{id} — CLIENT : annuler */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RestResponse<Void>> annuler(@PathVariable Long id, Principal principal) {
        service.annuler(id, principal.getName());
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
            .body(RestResponse.success(null, HttpStatus.NO_CONTENT));
    }

    private boolean isAdmin(Principal principal) {
        var auth = (org.springframework.security.core.Authentication) principal;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
