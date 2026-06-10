package sn.immosn.backend.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.notification.dto.NotificationResponseDto;
import sn.immosn.backend.notification.service.NotificationService;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "NOTIFICATIONS", description = "Gestion des notifications persistées — unread, historique, mark-as-read")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Notifications non-lues", description = "Retourne la liste des notifications non-lues de l'utilisateur connecté.")
    @GetMapping("/unread")
    public ResponseEntity<RestResponse<List<NotificationResponseDto>>> getUnread(Principal principal) {
        User user = resolveUser(principal);
        List<NotificationResponseDto> data = notificationService.getUnreadNotifications(user.getId());
        return ResponseEntity.ok(RestResponse.success(data, HttpStatus.OK));
    }

    @Operation(summary = "Compteur non-lus", description = "Retourne uniquement le nombre de notifications non-lues (badge).")
    @GetMapping("/unread/count")
    public ResponseEntity<RestResponse<Long>> countUnread(Principal principal) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(RestResponse.success(notificationService.countUnread(user.getId()), HttpStatus.OK));
    }

    @Operation(summary = "Historique paginé", description = "Retourne l'historique complet des notifications (lues et non-lues), paginé par date décroissante.")
    @GetMapping
    public ResponseEntity<PagedResponse<NotificationResponseDto>> getHistory(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        User user = resolveUser(principal);
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponseDto> result = notificationService.getHistory(user.getId(), pageable);
        return ResponseEntity.ok(PagedResponse.fromPage(result));
    }

    @Operation(summary = "Marquer une notification lue")
    @PostMapping("/{id}/read")
    public ResponseEntity<RestResponse<NotificationResponseDto>> markAsRead(
            @PathVariable Long id,
            Principal principal) {
        User user = resolveUser(principal);
        NotificationResponseDto dto = notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(RestResponse.success(dto, HttpStatus.OK));
    }

    @Operation(summary = "Tout marquer comme lu", description = "Marque toutes les notifications non-lues de l'utilisateur comme lues.")
    @PostMapping("/read-all")
    public ResponseEntity<RestResponse<Integer>> markAllAsRead(Principal principal) {
        User user = resolveUser(principal);
        int updated = notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(RestResponse.success(updated, HttpStatus.OK));
    }

    private User resolveUser(Principal principal) {
        if (principal instanceof org.springframework.security.core.Authentication auth
                && auth.getPrincipal() instanceof User user) {
            return user;
        }
        throw new IllegalStateException("Utilisateur non authentifié");
    }
}
