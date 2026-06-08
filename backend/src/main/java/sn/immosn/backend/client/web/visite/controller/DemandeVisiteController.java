package sn.immosn.backend.client.web.visite.controller;

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
import sn.immosn.backend.client.web.contrat.dto.ContratResponseDto;
import sn.immosn.backend.client.web.visite.dto.*;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;
import sn.immosn.backend.visite.service.DemandeVisiteService;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/visites")
@RequiredArgsConstructor
@Tag(name = "VISITES", description = "Gestion des demandes de visite immobilière : création, suivi des statuts et planification")
@SecurityRequirement(name = "bearerAuth")
public class DemandeVisiteController {

    private final DemandeVisiteService service;

    @Operation(
        summary = "Demander une visite",
        description = """
            Soumet une demande de visite pour une annonce spécifique.

            La demande est créée avec le statut **EN_ATTENTE**.
            L'administrateur peut ensuite l'accepter ou la refuser.

            Statuts possibles : `EN_ATTENTE` → `ACCEPTEE` / `REFUSEE` / `ANNULEE` → `TERMINEE`

            **Accès : CLIENT uniquement**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Demande de visite créée avec succès",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 201,
                      "data": {
                        "id": 12,
                        "clientNom": "Aminata Diallo",
                        "annonceLibelle": "Villa F5 - Almadies",
                        "annonceAdresse": "Route des Almadies, Villa 12",
                        "dateVisite": "2024-01-20T10:00:00",
                        "statut": "EN_ATTENTE",
                        "commentaire": "Je souhaite visiter avec mon architecte.",
                        "createdAt": "2024-01-15T11:30:00"
                      }
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Données invalides — annonce ou date manquante",
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
    public ResponseEntity<RestResponse<DemandeVisiteResponseDto>> create(
            @RequestBody @Valid DemandeVisiteCreateRequestDto request, Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error("Vous devez être connecté", HttpStatus.UNAUTHORIZED));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RestResponse.success(service.create(request, principal.getName()), HttpStatus.CREATED));
    }

    @Operation(
        summary = "Mes demandes de visite (vue client)",
        description = """
            Retourne la liste paginée des demandes de visite du client connecté,
            triée par date de création décroissante.

            Peut être filtrée par statut :
            `EN_ATTENTE` | `ACCEPTEE` | `REFUSEE` | `ANNULEE` | `TERMINEE`

            **Accès : CLIENT uniquement**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des demandes de visite du client",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle CLIENT requis",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/client")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<PagedResponse<DemandeVisiteResponseDto>> getClientVisites(
            @Parameter(description = "Numéro de page", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filtre optionnel par statut") @RequestParam(required = false) StatutDemandeVisite statut,
            Principal principal) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PagedResponse.fromPage(
            service.getClientVisites(principal.getName(), statut, pageable)));
    }

    @Operation(
        summary = "Toutes les demandes de visite (vue admin)",
        description = """
            Retourne la liste paginée de toutes les demandes de visite de la plateforme,
            triée par date de création décroissante.

            Peut être filtrée par statut pour faciliter la planification.

            **Accès : ADMIN ou SUPER_ADMIN**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste de toutes les demandes de visite",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<DemandeVisiteResponseDto>> getAllVisites(
            @Parameter(description = "Numéro de page", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filtre optionnel par statut") @RequestParam(required = false) StatutDemandeVisite statut) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PagedResponse.fromPage(service.getAllVisites(statut, pageable)));
    }

    @Operation(
        summary = "Changer le statut d'une demande de visite",
        description = """
            Met à jour le statut d'une demande de visite.

            **Règles selon le rôle :**
            - **ADMIN/SUPER_ADMIN** : peut passer à `ACCEPTEE`, `REFUSEE`, `TERMINEE`
            - **CLIENT** : peut seulement passer à `ANNULEE` (sa propre demande)

            **Accès : CLIENT (annulation) ou ADMIN/SUPER_ADMIN (acceptation/refus/termination)**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statut mis à jour avec succès",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {"success":true,"status":200,"data":{"id":12,"statut":"ACCEPTEE","commentaire":"Confirmé pour samedi 10h"}}"""))),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide ou transition de statut non autorisée",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — opération non permise pour ce rôle",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Demande de visite non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<RestResponse<DemandeVisiteResponseDto>> updateStatut(
            @Parameter(description = "Identifiant de la demande de visite", required = true, example = "12")
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

    @Operation(
        summary = "Modifier la date d'une visite",
        description = """
            Permet à un administrateur de reprogrammer la date d'une visite acceptée.

            Utile pour les modifications de planning sans changer le statut de la demande.

            **Accès : ADMIN ou SUPER_ADMIN**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Date de visite modifiée avec succès",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide ou date invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Demande de visite non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}/date")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RestResponse<DemandeVisiteResponseDto>> updateDate(
            @Parameter(description = "Identifiant de la demande de visite", required = true, example = "12")
            @PathVariable Long id,
            @RequestBody @Valid UpdateDateVisiteDto dto) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de demande de visite invalide", null));
        }
        return ResponseEntity.ok(RestResponse.success(service.updateDate(id, dto), HttpStatus.OK));
    }

    @Operation(
        summary = "Annuler une demande de visite",
        description = """
            Permet au client d'annuler sa demande de visite en attente ou acceptée.

            Le statut passe à **ANNULEE**. Une demande refusée ou déjà terminée
            ne peut pas être annulée.

            **Accès : CLIENT uniquement (sa propre demande)**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Demande annulée avec succès — aucun contenu retourné"),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide ou annulation non possible",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle CLIENT requis ou demande appartenant à un autre client",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Demande de visite non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RestResponse<Void>> annuler(
            @Parameter(description = "Identifiant de la demande de visite à annuler", required = true, example = "12")
            @PathVariable Long id, Principal principal) {
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

    @Operation(
        summary = "Clôturer une visite acceptée",
        description = """
            Permet à un administrateur de clôturer une visite dont le statut est **ACCEPTEE**.

            **Deux options :**
            - `SANS_SUITE` : le client n'est pas intéressé. Visite → `CLOTUREE_SANS_SUITE`, lead → `ABANDONNE`. Retourne `null`.
            - `AVEC_CONTRAT` : le client souhaite poursuivre. Visite → `CLOTUREE_AVEC_CONTRAT`, contrat créé automatiquement (client, annonce, lead, montant=prix annonce), lead → `CONVERTI`. Retourne le contrat créé.

            Pour `AVEC_CONTRAT`, `typeContrat` (VENTE ou LOCATION) est obligatoire.
            Pour LOCATION, `dureeLocationMois` est obligatoire.

            **Accès : ADMIN ou SUPER_ADMIN**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Visite clôturée avec succès",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide, visite non ACCEPTEE ou paramètres manquants",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Demande de visite non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}/cloture")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RestResponse<ContratResponseDto>> cloturerVisite(
            @Parameter(description = "Identifiant de la demande de visite", required = true, example = "12")
            @PathVariable Long id,
            @RequestBody @Valid CloturerVisiteDto dto) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de demande de visite invalide", null));
        }
        ContratResponseDto contrat = service.cloturerVisite(id, dto);
        return ResponseEntity.ok(RestResponse.success(contrat, HttpStatus.OK));
    }

    private boolean isAdmin(Principal principal) {
        var auth = (org.springframework.security.core.Authentication) principal;
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }
}
