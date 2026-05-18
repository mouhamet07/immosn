package sn.immosn.backend.client.web.discussion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.immosn.backend.client.web.discussion.dto.*;
import sn.immosn.backend.discussion.service.DiscussionService;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/discussions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class DiscussionController {

    private final DiscussionService discussionService;

    /**
     * POST /api/v1/discussions
     * Crée ou récupère une discussion depuis une annonce (CLIENT uniquement)
     */
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RestResponse<DiscussionResponseDto>> createOrGet(
            @RequestBody @Valid DiscussionCreateRequestDto request,
            Principal principal) {
        DiscussionResponseDto dto = discussionService.createOrGetDiscussion(request, principal.getName());
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(RestResponse.success(dto, HttpStatus.CREATED));
    }

    /**
     * GET /api/v1/discussions/client
     * Liste des discussions du client connecté
     */
    @GetMapping("/client")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<PagedResponse<DiscussionListDto>> getClientDiscussions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DiscussionListDto> result = discussionService.getClientDiscussions(principal.getName(), pageable);
        return ResponseEntity.ok(PagedResponse.fromPage(result));
    }

    /**
     * GET /api/v1/discussions/admin
     * Toutes les discussions (ADMIN uniquement)
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<DiscussionListDto>> getAllDiscussions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DiscussionListDto> result = discussionService.getAllDiscussions(pageable);
        return ResponseEntity.ok(PagedResponse.fromPage(result));
    }

    /**
     * GET /api/v1/discussions/{id}/messages
     * Récupère les messages d'une discussion (et marque les messages comme lus)
     */
    @GetMapping("/{id}/messages")
    public ResponseEntity<RestResponse<DiscussionResponseDto>> getMessages(
            @PathVariable Long id,
            Principal principal) {
        boolean isAdmin = isAdmin(principal);
        DiscussionResponseDto dto = discussionService.getDiscussion(id, principal.getName(), isAdmin);
        return ResponseEntity.ok(RestResponse.success(dto, HttpStatus.OK));
    }

    /**
     * POST /api/v1/discussions/{id}/messages
     * Envoie un message dans une discussion
     */
    @PostMapping("/{id}/messages")
    public ResponseEntity<RestResponse<MessageResponseDto>> sendMessage(
            @PathVariable Long id,
            @RequestBody @Valid MessageCreateRequestDto request,
            Principal principal) {
        boolean isAdmin = isAdmin(principal);
        MessageResponseDto dto = discussionService.sendMessage(id, request, principal.getName(), isAdmin);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(RestResponse.success(dto, HttpStatus.CREATED));
    }

    private boolean isAdmin(Principal principal) {
        org.springframework.security.core.Authentication auth =
            (org.springframework.security.core.Authentication) principal;
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
