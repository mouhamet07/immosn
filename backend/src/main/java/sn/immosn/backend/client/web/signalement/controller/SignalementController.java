package sn.immosn.backend.client.web.signalement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.immosn.backend.client.web.signalement.dto.*;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;
import sn.immosn.backend.signalement.data.entity.StatutSignalement;
import sn.immosn.backend.signalement.service.SignalementService;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/signalements")
@RequiredArgsConstructor
public class SignalementController {

    private final SignalementService service;

    /** POST — CLIENT : créer un signalement */
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RestResponse<SignalementResponseDto>> create(
            @RequestBody @Valid SignalementCreateRequestDto request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RestResponse.success(service.create(request, principal.getName()), HttpStatus.CREATED));
    }

    /** GET /client */
    @GetMapping("/client")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<PagedResponse<SignalementResponseDto>> getClientSignalements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) StatutSignalement statut,
            Principal principal) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PagedResponse.fromPage(
            service.getClientSignalements(principal.getName(), statut, pageable)));
    }

    /** GET /admin */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<SignalementResponseDto>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) StatutSignalement statut) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PagedResponse.fromPage(service.getAll(statut, pageable)));
    }

    /** PUT /{id}/status — ADMIN */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<SignalementResponseDto>> updateStatut(
            @PathVariable Long id, @RequestBody @Valid UpdateStatutSignalementDto dto) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de signalement invalide", null));
        }
        return ResponseEntity.ok(RestResponse.success(service.updateStatut(id, dto), HttpStatus.OK));
    }

    /** PUT /{id}/read — ADMIN */
    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<Void>> markAsRead(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de signalement invalide", null));
        }
        service.markAsRead(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
            .body(RestResponse.success(null, HttpStatus.NO_CONTENT));
    }
}
