package sn.immosn.backend.client.web.contrat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.immosn.backend.client.web.contrat.dto.*;
import sn.immosn.backend.contrat.data.entity.StatutContrat;
import sn.immosn.backend.contrat.service.ContratService;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/contrats")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ContratController {

    private final ContratService service;

    /** POST — ADMIN : créer un contrat */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<ContratResponseDto>> create(@RequestBody @Valid ContratCreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RestResponse.success(service.create(request), HttpStatus.CREATED));
    }

    /** GET /client — CLIENT : ses contrats */
    @GetMapping("/client")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<PagedResponse<ContratResponseDto>> getClientContrats(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) StatutContrat statut,
            Principal principal) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PagedResponse.fromPage(
            service.getClientContrats(principal.getName(), statut, pageable)));
    }

    /** GET /admin — ADMIN : tous les contrats */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<ContratResponseDto>> getAllContrats(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) StatutContrat statut) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PagedResponse.fromPage(service.getAllContrats(statut, pageable)));
    }

    /** GET /{id} */
    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<ContratResponseDto>> getById(
            @PathVariable Long id, Principal principal) {
        boolean isAdmin = isAdmin(principal);
        return ResponseEntity.ok(RestResponse.success(
            service.getById(id, principal.getName(), isAdmin), HttpStatus.OK));
    }

    /** PUT /{id} — ADMIN : modification */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<ContratResponseDto>> update(
            @PathVariable Long id, @RequestBody @Valid ContratUpdateRequestDto request) {
        return ResponseEntity.ok(RestResponse.success(service.update(id, request), HttpStatus.OK));
    }

    /** PUT /{id}/resiliation — CLIENT : demande de résiliation */
    @PutMapping("/{id}/resiliation")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RestResponse<ContratResponseDto>> demanderResiliation(
            @PathVariable Long id, @RequestBody ContratActionDto dto, Principal principal) {
        return ResponseEntity.ok(RestResponse.success(
            service.demanderResiliation(id, dto, principal.getName()), HttpStatus.OK));
    }

    /** PUT /{id}/prolongation — CLIENT : demande de prolongation */
    @PutMapping("/{id}/prolongation")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RestResponse<ContratResponseDto>> demanderProlongation(
            @PathVariable Long id, @RequestBody ContratActionDto dto, Principal principal) {
        return ResponseEntity.ok(RestResponse.success(
            service.demanderProlongation(id, dto, principal.getName()), HttpStatus.OK));
    }

    private boolean isAdmin(Principal principal) {
        var auth = (org.springframework.security.core.Authentication) principal;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
