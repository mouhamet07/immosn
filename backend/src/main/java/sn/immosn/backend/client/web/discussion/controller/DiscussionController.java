package sn.immosn.backend.client.web.discussion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "DISCUSSIONS", description = "Messagerie entre clients et agents immobiliers, organisée par annonce")
@SecurityRequirement(name = "bearerAuth")
public class DiscussionController {

    private final DiscussionService discussionService;

    @Operation(
        summary = "Démarrer ou récupérer une discussion",
        description = """
            Crée une discussion entre le client connecté et l'équipe ImmoSN à propos d'une annonce,
            ou retourne la discussion existante si elle existe déjà (idempotent).

            La contrainte d'unicité est `(client_id, annonce_id)` : un client ne peut avoir
            qu'une seule discussion par annonce.

            Le premier message est envoyé dans le corps de la requête.

            **Accès : CLIENT uniquement**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Discussion créée (ou existante retournée) avec le premier message",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 201,
                      "data": {
                        "id": 7,
                        "annonceId": 15,
                        "annonceLibelle": "Villa F5 - Almadies",
                        "clientNom": "Aminata Diallo",
                        "messages": [
                          {
                            "id": 1,
                            "contenu": "Bonjour, est-il possible de visiter cette villa ce weekend ?",
                            "senderRole": "CLIENT",
                            "senderName": "Aminata Diallo",
                            "isRead": false,
                            "createdAt": "2024-01-15T11:00:00"
                          }
                        ],
                        "unreadCount": 0,
                        "createdAt": "2024-01-15T11:00:00"
                      }
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Données invalides — annonce ou message manquant",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle CLIENT requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Annonce non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RestResponse<DiscussionResponseDto>> createOrGet(
            @RequestBody @Valid DiscussionCreateRequestDto request,
            Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error("Vous devez être connecté", HttpStatus.UNAUTHORIZED));
        }
        DiscussionResponseDto dto = discussionService.createOrGetDiscussion(request, principal.getName());
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(RestResponse.success(dto, HttpStatus.CREATED));
    }

    @Operation(
        summary = "Mes discussions (vue client)",
        description = """
            Retourne la liste paginée des discussions du client connecté,
            incluant le dernier message et le compteur de messages non lus par discussion.

            **Accès : CLIENT uniquement**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des discussions du client",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "content": [
                        {
                          "id": 7,
                          "annonceLibelle": "Villa F5 - Almadies",
                          "clientNom": "Aminata Diallo",
                          "dernierMessage": "Bonjour, disponible samedi à 10h.",
                          "dernierMessageRole": "ADMIN",
                          "unreadCount": 1,
                          "dernierMessageAt": "2024-01-15T14:30:00"
                        }
                      ],
                      "page": 0, "size": 10, "totalElements": 3, "totalPages": 1, "last": true
                    }"""))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle CLIENT requis",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/client")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<PagedResponse<DiscussionListDto>> getClientDiscussions(
            @Parameter(description = "Numéro de page", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page", example = "10") @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DiscussionListDto> result = discussionService.getClientDiscussions(principal.getName(), pageable);
        return ResponseEntity.ok(PagedResponse.fromPage(result));
    }

    @Operation(
        summary = "Toutes les discussions (vue admin)",
        description = """
            Retourne la liste paginée de toutes les discussions de la plateforme,
            avec les indicateurs de messages non lus pour chaque fil.

            **Accès : ADMIN ou SUPER_ADMIN**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste de toutes les discussions",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<DiscussionListDto>> getAllDiscussions(
            @Parameter(description = "Numéro de page", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page", example = "20") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DiscussionListDto> result = discussionService.getAllDiscussions(pageable);
        return ResponseEntity.ok(PagedResponse.fromPage(result));
    }

    @Operation(
        summary = "Lire les messages d'une discussion",
        description = """
            Retourne la discussion complète avec tous ses messages.

            Les messages non lus sont automatiquement marqués comme lus lors de cet appel
            (côté client pour l'admin, côté admin pour le client).

            **Accès : CLIENT (ses discussions) ou ADMIN/SUPER_ADMIN (toutes)**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Discussion avec tous ses messages",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 200,
                      "data": {
                        "id": 7,
                        "annonceLibelle": "Villa F5 - Almadies",
                        "clientNom": "Aminata Diallo",
                        "messages": [
                          {"id": 1, "contenu": "Bonjour, disponible pour visiter ce weekend ?", "senderRole": "CLIENT", "isRead": true},
                          {"id": 2, "contenu": "Bonjour, disponible samedi à 10h.", "senderRole": "ADMIN", "isRead": true}
                        ],
                        "unreadCount": 0
                      }
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — le client consulte une discussion qui n'est pas la sienne",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Discussion non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}/messages")
    public ResponseEntity<RestResponse<DiscussionResponseDto>> getMessages(
            @Parameter(description = "Identifiant de la discussion", required = true, example = "7")
            @PathVariable Long id,
            Principal principal) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de discussion invalide", null));
        }
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error("Vous devez être connecté", HttpStatus.UNAUTHORIZED));
        }
        boolean isAdmin = isAdmin(principal);
        DiscussionResponseDto dto = discussionService.getDiscussion(id, principal.getName(), isAdmin);
        return ResponseEntity.ok(RestResponse.success(dto, HttpStatus.OK));
    }

    @Operation(
        summary = "Envoyer un message",
        description = """
            Envoie un nouveau message dans une discussion existante.

            Le rôle de l'expéditeur (`senderRole`) est déterminé automatiquement
            à partir du token JWT : `CLIENT` ou `ADMIN`.

            **Accès : CLIENT (ses discussions) ou ADMIN/SUPER_ADMIN (toutes)**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Message envoyé avec succès",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 201,
                      "data": {
                        "id": 3,
                        "contenu": "Parfait, nous confirmons le rendez-vous samedi 10h.",
                        "senderRole": "ADMIN",
                        "senderName": "Ibrahima Sow",
                        "isRead": false,
                        "createdAt": "2024-01-15T15:00:00"
                      }
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide ou contenu du message vide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — le client envoie dans une discussion qui n'est pas la sienne",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Discussion non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/{id}/messages")
    public ResponseEntity<RestResponse<MessageResponseDto>> sendMessage(
            @Parameter(description = "Identifiant de la discussion", required = true, example = "7")
            @PathVariable Long id,
            @RequestBody @Valid MessageCreateRequestDto request,
            Principal principal) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de discussion invalide", null));
        }
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error("Vous devez être connecté", HttpStatus.UNAUTHORIZED));
        }
        boolean isAdmin = isAdmin(principal);
        MessageResponseDto dto = discussionService.sendMessage(id, request, principal.getName(), isAdmin);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(RestResponse.success(dto, HttpStatus.CREATED));
    }

    private boolean isAdmin(Principal principal) {
        var auth = (org.springframework.security.core.Authentication) principal;
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                       || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }
}
