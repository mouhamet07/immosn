package sn.immosn.backend.client.web.lead.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.immosn.backend.client.web.lead.dto.*;
import sn.immosn.backend.lead.data.entity.StatutLead;
import sn.immosn.backend.lead.service.LeadService;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
@PreAuthorize("hasRole('ADMIN')")
public class LeadController {

    private final LeadService service;

    @PostMapping
    public ResponseEntity<RestResponse<LeadResponseDto>> create(@RequestBody @Valid LeadCreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RestResponse.success(service.create(request), HttpStatus.CREATED));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<LeadResponseDto>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) StatutLead statut) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PagedResponse.fromPage(service.getAll(statut, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<LeadResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(RestResponse.success(service.getById(id), HttpStatus.OK));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<RestResponse<LeadResponseDto>> updateStatut(
            @PathVariable Long id,
            @RequestBody @Valid UpdateStatutLeadDto dto) {
        return ResponseEntity.ok(RestResponse.success(service.updateStatut(id, dto), HttpStatus.OK));
    }
}
