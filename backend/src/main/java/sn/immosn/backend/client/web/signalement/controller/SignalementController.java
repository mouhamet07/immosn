package sn.immosn.backend.client.web.signalement.controller;

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
@Tag(name = "SIGNALEMENTS", description = "Gestion des signalements et litiges liés aux contrats : soumission par les clients, traitement par les admins")
@SecurityRequirement(name = "bearerAuth")
public class SignalementController {

    private final SignalementService service;

    @Operation(
        summary = "Créer un signalement",
        description = """
            Permet à un client de signaler un problème lié à un contrat actif
            (non-respect des conditions, problème logement, litige de paiement, etc.).

            Le signalement est créé avec le statut **OUVERT** et `isRead: false`.

            Statuts possibles : `OUVERT` → `EN_COURS` → `RESOLU` / `FERME`

            **Accès : CLIENT uniquement**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Signalement créé avec succès",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 201,
                      "data": {
                        "id": 5,
                        "contratId": 8,
                        "annonceLibelle": "Villa F5 - Almadies",
                        "clientNom": "Aminata Diallo",
                        "contenu": "La villa ne correspond pas à la description du contrat.",
                        "statut": "OUVERT",
                        "isRead": false,
                        "reponseAdmin": null,
                        "createdAt": "2024-02-10T09:00:00"
                      }
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Données invalides — contrat ou contenu manquant",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle CLIENT requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Contrat non trouvé ou n'appartient pas au client",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RestResponse<SignalementResponseDto>> create(
            @RequestBody @Valid SignalementCreateRequestDto request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RestResponse.success(service.create(request, principal.getName()), HttpStatus.CREATED));
    }

    @Operation(
        summary = "Mes signalements (vue client)",
        description = """
            Retourne la liste paginée des signalements du client connecté,
            triée par date de création décroissante.

            Peut être filtrée par statut :
            `OUVERT` | `EN_COURS` | `RESOLU` | `FERME`

            **Accès : CLIENT uniquement**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des signalements du client",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle CLIENT requis",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/client")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<PagedResponse<SignalementResponseDto>> getClientSignalements(
            @Parameter(description = "Numéro de page", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filtre optionnel par statut") @RequestParam(required = false) StatutSignalement statut,
            Principal principal) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PagedResponse.fromPage(
            service.getClientSignalements(principal.getName(), statut, pageable)));
    }

    @Operation(
        summary = "Tous les signalements (vue admin)",
        description = """
            Retourne la liste paginée de tous les signalements de la plateforme,
            triée par date de création décroissante.

            Peut être filtrée par statut. Les signalements non lus (`isRead: false`)
            doivent être traités en priorité.

            **Accès : ADMIN uniquement**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste de tous les signalements",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN requis",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<SignalementResponseDto>> getAll(
            @Parameter(description = "Numéro de page", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filtre optionnel par statut") @RequestParam(required = false) StatutSignalement statut) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PagedResponse.fromPage(service.getAll(statut, pageable)));
    }

    @Operation(
        summary = "Mettre à jour le statut d'un signalement",
        description = """
            Permet à un administrateur de traiter un signalement en changeant son statut
            et en fournissant une réponse au client.

            Transitions de statut typiques :
            - `OUVERT` → `EN_COURS` (prise en charge)
            - `EN_COURS` → `RESOLU` (problème résolu, réponse fournie)
            - `EN_COURS` → `FERME` (signalement non fondé)

            **Accès : ADMIN uniquement**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statut du signalement mis à jour",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 200,
                      "data": {
                        "id": 5,
                        "statut": "RESOLU",
                        "reponseAdmin": "Le problème a été constaté et corrigé. Nos excuses pour la gêne occasionnée.",
                        "isRead": true
                      }
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Signalement non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<SignalementResponseDto>> updateStatut(
            @Parameter(description = "Identifiant du signalement", required = true, example = "5")
            @PathVariable Long id, @RequestBody @Valid UpdateStatutSignalementDto dto) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de signalement invalide", null));
        }
        return ResponseEntity.ok(RestResponse.success(service.updateStatut(id, dto), HttpStatus.OK));
    }

    @Operation(
        summary = "Marquer un signalement comme lu",
        description = """
            Marque un signalement comme lu par l'administrateur (`isRead: true`).

            Appelé automatiquement quand l'admin ouvre un signalement dans le tableau de bord.
            Permet de réduire le compteur de signalements non lus.

            **Accès : ADMIN uniquement**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Signalement marqué comme lu — aucun contenu retourné"),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Signalement non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<Void>> markAsRead(
            @Parameter(description = "Identifiant du signalement à marquer lu", required = true, example = "5")
            @PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de signalement invalide", null));
        }
        service.markAsRead(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
            .body(RestResponse.success(null, HttpStatus.NO_CONTENT));
    }
}
