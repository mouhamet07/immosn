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
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sn.immosn.backend.client.web.contrat.dto.ContratResponseDto;
import sn.immosn.backend.client.web.visite.dto.*;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;
import sn.immosn.backend.visite.service.DemandeVisiteService;
import sn.immosn.backend.visite.service.RapportVisiteService;
import sn.immosn.backend.visite.service.VisiteHistoryService;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/visites")
@RequiredArgsConstructor
@Tag(name = "VISITES", description = "Gestion des demandes de visite immobilière : création, suivi des statuts et planification")
@SecurityRequirement(name = "bearerAuth")
public class DemandeVisiteController {

    private final DemandeVisiteService service;
    private final VisiteHistoryService historyService;
    private final RapportVisiteService rapportService;

    @Operation(
        summary = "Demander une visite",
        description = """
            Soumet une demande de visite pour une annonce spécifique.

            La demande est créée avec le statut **EN_ATTENTE**.
            L'administrateur peut ensuite l'accepter ou la refuser.

            Statuts possibles : `EN_ATTENTE` → `ACCEPTEE` / `REFUSEE` / `ANNULEE` → `CLOTUREE_SANS_SUITE` / `CLOTUREE_AVEC_CONTRAT`

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
        summary = "Demander une visite (visiteur non authentifié)",
        description = """
            Soumet une demande de visite sans compte. Le visiteur fournit son identité
            (nom, prénom, téléphone, email, adresse) et le créneau souhaité.

            La demande est créée avec le statut **EN_ATTENTE** et rattachée à un prospect
            (créé ou réutilisé par email). La réponse contient un `prospectToken` de suivi
            que le visiteur doit conserver.

            **Accès : PUBLIC — aucun token requis**
            """,
        security = {}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Demande de visite invité créée avec succès",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Données invalides",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Annonce non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/invite")
    public ResponseEntity<RestResponse<VisiteInviteResponseDto>> createInvite(
            @RequestBody @Valid VisiteInviteCreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RestResponse.success(service.createInvite(request), HttpStatus.CREATED));
    }

    @Operation(
        summary = "Suivre une visite par token (visiteur non authentifié)",
        description = """
            Retourne le statut, la date prévue, l'administrateur affecté et l'historique
            de toutes les demandes de visite rattachées au numéro de suivi (token du prospect).

            **Accès : PUBLIC — aucun token JWT requis**
            """,
        security = {}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Suivi de la visite",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Aucun suivi trouvé pour ce numéro",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/suivi/{token}")
    public ResponseEntity<RestResponse<SuiviVisiteResponseDto>> suivreParToken(@PathVariable String token) {
        return ResponseEntity.ok(RestResponse.success(service.suivreParToken(token), HttpStatus.OK));
    }

    @Operation(
        summary = "Mes demandes de visite (vue client)",
        description = """
            Retourne la liste paginée des demandes de visite du client connecté,
            triée par date de création décroissante.

            Peut être filtrée par statut :
            `EN_ATTENTE` | `ACCEPTEE` | `REFUSEE` | `ANNULEE` | `CLOTUREE_SANS_SUITE` | `CLOTUREE_AVEC_CONTRAT` | `TERMINEE` (historique)

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
            @Parameter(description = "Filtre optionnel par statut") @RequestParam(required = false) StatutDemandeVisite statut,
            @Parameter(description = "Filtre optionnel par type de transaction (VENTE/LOCATION)") @RequestParam(required = false) sn.immosn.backend.annonce.data.entity.TypeTransaction typeTransaction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PagedResponse.fromPage(service.getAllVisites(statut, typeTransaction, pageable)));
    }

    @Operation(
        summary = "Détail d'une demande de visite",
        description = """
            Retourne les informations complètes d'une demande de visite.

            - Un **CLIENT** ne peut consulter que ses propres demandes.
            - Un **ADMIN** peut consulter toutes les demandes.

            **Accès : CLIENT (ses demandes) ou ADMIN/SUPER_ADMIN (toutes)**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Détail de la demande de visite",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Demande de visite non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<DemandeVisiteResponseDto>> getById(
            @Parameter(description = "Identifiant de la demande de visite", required = true, example = "12")
            @PathVariable Long id, Principal principal) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant invalide", null));
        }
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error("Vous devez être connecté", HttpStatus.UNAUTHORIZED));
        }
        boolean isAdmin = isAdmin(principal);
        return ResponseEntity.ok(RestResponse.success(
            service.getById(id, principal.getName(), isAdmin), HttpStatus.OK));
    }

    @Operation(
        summary = "Changer le statut d'une demande de visite",
        description = """
            Met à jour le statut d'une demande de visite.

            **Règles selon le rôle :**
            - **ADMIN/SUPER_ADMIN** : `EN_ATTENTE` → `ACCEPTEE` ou `EN_ATTENTE` → `REFUSEE` uniquement. Pour clôturer une visite ACCEPTEE, utiliser `PUT /{id}/cloture`.
            - **CLIENT** : `EN_ATTENTE` → `ANNULEE` ou `ACCEPTEE` → `ANNULEE` (sa propre demande uniquement).

            `TERMINEE` est un statut historique en lecture seule — il ne peut plus être défini via l'API.

            **Accès : CLIENT (annulation) ou ADMIN/SUPER_ADMIN (acceptation/refus)**
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
        // Sprint 2 : l'acceptation et le refus d'une visite sont réservés au SUPER_ADMIN.
        // L'annulation reste ouverte au CLIENT (sa propre demande).
        if (isAdmin
                && (dto.statut() == StatutDemandeVisite.ACCEPTEE || dto.statut() == StatutDemandeVisite.REFUSEE)
                && !isSuperAdmin(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(RestResponse.forbidden("Seul un SUPER_ADMIN peut accepter ou refuser une demande de visite."));
        }
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
        summary = "Modifier une demande de visite (client)",
        description = """
            Permet au client de modifier la date souhaitée et/ou le commentaire
            de sa demande de visite, **uniquement si elle est EN_ATTENTE**.

            Une visite déjà acceptée, refusée ou clôturée ne peut plus être modifiée par le client.

            **Accès : CLIENT uniquement (sa propre demande)**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Demande de visite modifiée avec succès",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Statut incompatible avec une modification",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Demande de visite non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}/modifier")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RestResponse<DemandeVisiteResponseDto>> modifierParClient(
            @PathVariable Long id,
            @RequestBody @Valid UpdateDateVisiteDto dto,
            Principal principal) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant invalide", null));
        }
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error("Vous devez être connecté", HttpStatus.UNAUTHORIZED));
        }
        return ResponseEntity.ok(RestResponse.success(
            service.modifierParClient(id, dto, principal.getName()), HttpStatus.OK));
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

            Pour `AVEC_CONTRAT`, le type de contrat (VENTE ou LOCATION) est déterminé automatiquement
            par le `typeTransaction` de l'annonce — il ne se choisit plus manuellement.
            Pour une annonce LOCATION, `dureeLocationMois` est obligatoire.

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

    @Operation(summary = "Historique d'une visite", description = "Retourne l'historique complet des transitions et reprogrammations d'une visite. **Accès : ADMIN ou SUPER_ADMIN**")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historique de la visite",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Visite non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}/historique")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<VisiteHistoryDto>> getHistorique(
            @Parameter(description = "Identifiant de la visite", required = true) @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(PagedResponse.fromPage(historyService.getHistory(id, pageable)));
    }

    // Sprint 2 — affectation, replanification, rapport de visite

    @Operation(summary = "Affecter un administrateur responsable",
        description = "Affecte un administrateur à une visite ACCEPTEE (→ AFFECTEE). **Accès : SUPER_ADMIN**")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Visite affectée",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Transition invalide ou utilisateur non admin",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Visite ou administrateur non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}/affecter")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<RestResponse<DemandeVisiteResponseDto>> affecter(
            @PathVariable Long id, @RequestBody @Valid AffecterAdminDto dto) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant invalide", null));
        }
        return ResponseEntity.ok(RestResponse.success(service.affecterAdmin(id, dto), HttpStatus.OK));
    }

    @Operation(summary = "Demander une replanification",
        description = "Mémorise une nouvelle date proposée (→ REPLANIFICATION_DEMANDEE). **Accès : SUPER_ADMIN**")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Replanification demandée",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Transition invalide ou date passée",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Visite non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}/replanification")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<RestResponse<DemandeVisiteResponseDto>> demanderReplanification(
            @PathVariable Long id, @RequestBody @Valid ReplanificationDto dto) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant invalide", null));
        }
        return ResponseEntity.ok(RestResponse.success(service.demanderReplanification(id, dto), HttpStatus.OK));
    }

    @Operation(summary = "Accepter la replanification",
        description = "Applique la date proposée et revient à l'état actif. **Accès : SUPER_ADMIN**")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Replanification acceptée",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Aucune replanification en attente",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Visite non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}/replanification/accepter")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<RestResponse<DemandeVisiteResponseDto>> accepterReplanification(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant invalide", null));
        }
        return ResponseEntity.ok(RestResponse.success(service.accepterReplanification(id), HttpStatus.OK));
    }

    @Operation(summary = "Rédiger le rapport de visite",
        description = "Crée le rapport et fait passer la visite à RAPPORT_REDIGE. **Accès : ADMIN ou SUPER_ADMIN**")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Rapport créé",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Statut incompatible ou rapport déjà existant",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Visite non trouvée",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/{id}/rapport")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RestResponse<RapportVisiteResponseDto>> creerRapport(
            @PathVariable Long id,
            @RequestBody @Valid RapportVisiteCreateRequestDto dto,
            Principal principal) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant invalide", null));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RestResponse.success(rapportService.creerRapport(id, dto, principal.getName()), HttpStatus.CREATED));
    }

    @Operation(summary = "Consulter le rapport de visite",
        description = "Retourne le rapport associé à la visite. **Accès : ADMIN ou SUPER_ADMIN**")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rapport de la visite",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Aucun rapport pour cette visite",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}/rapport")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RestResponse<RapportVisiteResponseDto>> consulterRapport(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant invalide", null));
        }
        return ResponseEntity.ok(RestResponse.success(rapportService.consulterRapport(id), HttpStatus.OK));
    }

    @Operation(summary = "Téléverser le document du rapport",
        description = "Associe un document (PDF/PNG/JPG, max 10 Mo) au rapport. **Accès : ADMIN ou SUPER_ADMIN**")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Document téléversé",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Fichier invalide ou format non supporté",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Aucun rapport pour cette visite",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping(value = "/{id}/rapport/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RestResponse<RapportVisiteResponseDto>> uploadRapportDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant invalide", null));
        }
        return ResponseEntity.ok(RestResponse.success(rapportService.uploadDocument(id, file), HttpStatus.OK));
    }

    private boolean isAdmin(Principal principal) {
        var auth = (org.springframework.security.core.Authentication) principal;
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    private boolean isSuperAdmin(Principal principal) {
        var auth = (org.springframework.security.core.Authentication) principal;
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }
}
