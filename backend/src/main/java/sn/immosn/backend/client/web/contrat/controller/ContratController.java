package sn.immosn.backend.client.web.contrat.controller;

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
import sn.immosn.backend.client.web.contrat.dto.*;
import sn.immosn.backend.contrat.data.entity.StatutContrat;
import sn.immosn.backend.contrat.service.ContratHistoryService;
import sn.immosn.backend.contrat.service.ContratService;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/contrats")
@RequiredArgsConstructor
@Tag(name = "CONTRATS", description = "Gestion des contrats de location/vente : création, suivi des statuts, demandes de résiliation et de prolongation")
@SecurityRequirement(name = "bearerAuth")
public class ContratController {

    private final ContratService        service;
    private final ContratHistoryService historyService;

    @Operation(
        summary = "Créer un contrat",
        description = """
            Crée un nouveau contrat entre un client et une annonce.

            Le contrat est créé avec le statut **EN_ATTENTE** par défaut.
            Il peut optionnellement être lié à un lead et/ou une demande de visite.

            Statuts possibles : `EN_ATTENTE` → `ACTIF` → `EXPIRE` / `RESILIE`

            **Accès : ADMIN uniquement**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Contrat créé avec succès",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 201,
                      "data": {
                        "id": 8,
                        "clientNom": "Aminata Diallo",
                        "annonceLibelle": "Villa F5 - Almadies",
                        "dateDebut": "2024-02-01",
                        "dateFin": "2025-01-31",
                        "montant": 450000,
                        "statut": "EN_ATTENTE",
                        "createdAt": "2024-01-20T14:00:00"
                      }
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Données invalides — champs obligatoires manquants",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Client, annonce ou lead non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RestResponse<ContratResponseDto>> create(@RequestBody @Valid ContratCreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RestResponse.success(service.create(request), HttpStatus.CREATED));
    }

    @Operation(
        summary = "Mes contrats (vue client)",
        description = """
            Retourne la liste paginée des contrats du client connecté,
            triée par date de création décroissante.

            Peut être filtrée par statut :
            `EN_ATTENTE` | `ACTIF` | `EXPIRE` | `RESILIE` | `EN_ATTENTE_RESILIATION` | `PROLONGATION_EN_ATTENTE`

            **Accès : CLIENT uniquement**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des contrats du client",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle CLIENT requis",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/client")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<PagedResponse<ContratResponseDto>> getClientContrats(
            @Parameter(description = "Numéro de page", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filtre optionnel par statut du contrat") @RequestParam(required = false) StatutContrat statut,
            Principal principal) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PagedResponse.fromPage(
            service.getClientContrats(principal.getName(), statut, pageable)));
    }

    @Operation(
        summary = "Tous les contrats (vue admin)",
        description = """
            Retourne la liste paginée de tous les contrats de la plateforme,
            triée par date de création décroissante.

            Peut être filtrée par statut pour faciliter le suivi.

            **Accès : ADMIN uniquement**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste complète des contrats",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN requis",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<ContratResponseDto>> getAllContrats(
            @Parameter(description = "Numéro de page", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filtre optionnel par statut") @RequestParam(required = false) StatutContrat statut) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PagedResponse.fromPage(service.getAllContrats(statut, pageable)));
    }

    @Operation(
        summary = "Détail d'un contrat",
        description = """
            Retourne les informations complètes d'un contrat.

            - Un **CLIENT** ne peut consulter que ses propres contrats
            - Un **ADMIN** peut consulter tous les contrats

            **Accès : CLIENT (ses contrats) ou ADMIN (tous)**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Détail du contrat",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 200,
                      "data": {
                        "id": 8,
                        "clientId": 42,
                        "clientNom": "Aminata Diallo",
                        "annonceId": 15,
                        "annonceLibelle": "Villa F5 - Almadies",
                        "annonceAdresse": "Route des Almadies, Villa 12",
                        "imagePrincipale": "https://storage.immosn.sn/annonces/15/img1.jpg",
                        "dateDebut": "2024-02-01",
                        "dateFin": "2025-01-31",
                        "montant": 450000,
                        "statut": "ACTIF",
                        "documentUrl": "https://storage.immosn.sn/contrats/8.pdf",
                        "notes": "Paiement mensuel le 1er du mois"
                      }
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — le client tente de consulter un contrat qui n'est pas le sien",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Contrat non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<ContratResponseDto>> getById(
            @Parameter(description = "Identifiant du contrat", required = true, example = "8")
            @PathVariable Long id, Principal principal) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de contrat invalide", null));
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
        summary = "Modifier un contrat",
        description = """
            Met à jour les informations d'un contrat existant.

            Permet notamment de changer le statut, ajuster les dates, le montant
            ou associer un document contractuel.

            **Accès : ADMIN uniquement**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contrat modifié avec succès",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide ou données incorrectes",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Contrat non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RestResponse<ContratResponseDto>> update(
            @Parameter(description = "Identifiant du contrat à modifier", required = true, example = "8")
            @PathVariable Long id, @RequestBody @Valid ContratUpdateRequestDto request) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de contrat invalide", null));
        }
        return ResponseEntity.ok(RestResponse.success(service.update(id, request), HttpStatus.OK));
    }

    @Operation(
        summary = "Demander la résiliation d'un contrat",
        description = """
            Le client soumet une demande de résiliation de son contrat actif.

            Le statut passe à **EN_ATTENTE_RESILIATION** et un motif peut être fourni.
            L'administrateur traite ensuite la demande en modifiant le statut via `PUT /{id}`.

            **Accès : CLIENT uniquement (son propre contrat)**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Demande de résiliation enregistrée — statut : EN_ATTENTE_RESILIATION",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {"success":true,"status":200,"data":{"id":8,"statut":"EN_ATTENTE_RESILIATION"}}"""))),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide ou contrat non résiliable dans son état actuel",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle CLIENT requis ou contrat appartenant à un autre client",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Contrat non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}/resiliation")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RestResponse<ContratResponseDto>> demanderResiliation(
            @Parameter(description = "Identifiant du contrat", required = true, example = "8")
            @PathVariable Long id, @RequestBody ContratActionDto dto, Principal principal) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de contrat invalide", null));
        }
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error("Vous devez être connecté", HttpStatus.UNAUTHORIZED));
        }
        return ResponseEntity.ok(RestResponse.success(
            service.demanderResiliation(id, dto, principal.getName()), HttpStatus.OK));
    }

    @Operation(
        summary = "Demander la prolongation d'un contrat",
        description = """
            Le client soumet une demande de prolongation de son contrat.

            Le statut passe à **PROLONGATION_EN_ATTENTE**. Une nouvelle date de fin
            peut être proposée dans le corps de la requête.
            L'administrateur traite la demande via `PUT /{id}`.

            **Accès : CLIENT uniquement (son propre contrat)**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Demande de prolongation enregistrée — statut : PROLONGATION_EN_ATTENTE",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {"success":true,"status":200,"data":{"id":8,"statut":"PROLONGATION_EN_ATTENTE"}}"""))),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide ou contrat non prolongeable",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle CLIENT requis ou contrat appartenant à un autre client",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Contrat non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}/prolongation")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RestResponse<ContratResponseDto>> demanderProlongation(
            @Parameter(description = "Identifiant du contrat", required = true, example = "8")
            @PathVariable Long id, @RequestBody ContratActionDto dto, Principal principal) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de contrat invalide", null));
        }
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error("Vous devez être connecté", HttpStatus.UNAUTHORIZED));
        }
        return ResponseEntity.ok(RestResponse.success(
            service.demanderProlongation(id, dto, principal.getName()), HttpStatus.OK));
    }

    @Operation(
        summary = "Accepter une demande de résiliation",
        description = """
            L'administrateur accepte la demande de résiliation du client.

            Transition : `EN_ATTENTE_RESILIATION` → `RESILIE`

            Un commentaire admin optionnel peut être fourni dans le corps de la requête (`motif`).

            **Accès : ADMIN ou SUPER_ADMIN**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Résiliation acceptée — statut : RESILIE",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Transition invalide ou identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Contrat non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}/resiliation/accepter")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RestResponse<ContratResponseDto>> accepterResiliation(
            @PathVariable Long id, @RequestBody(required = false) ContratActionDto dto) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de contrat invalide", null));
        }
        return ResponseEntity.ok(RestResponse.success(service.accepterResiliation(id, dto), HttpStatus.OK));
    }

    @Operation(
        summary = "Refuser une demande de résiliation",
        description = """
            L'administrateur refuse la demande de résiliation du client.

            Transition : `EN_ATTENTE_RESILIATION` → `ACTIF`

            Un commentaire admin optionnel peut être fourni dans le corps de la requête (`motif`).

            **Accès : ADMIN ou SUPER_ADMIN**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Résiliation refusée — statut : ACTIF",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Transition invalide ou identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Contrat non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}/resiliation/refuser")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RestResponse<ContratResponseDto>> refuserResiliation(
            @PathVariable Long id, @RequestBody(required = false) ContratActionDto dto) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de contrat invalide", null));
        }
        return ResponseEntity.ok(RestResponse.success(service.refuserResiliation(id, dto), HttpStatus.OK));
    }

    @Operation(
        summary = "Accepter une demande de prolongation",
        description = """
            L'administrateur accepte la demande de prolongation du client.

            Transition : `PROLONGATION_EN_ATTENTE` → `ACTIF`

            **Pour VENTE :** fournir `nouvelleDate` pour mettre à jour la date de fin.

            **Pour LOCATION :** fournir `nouvelleDate` — la durée en mois et le montant total
            sont recalculés automatiquement (`prix × nouvelleDurée`).
            La nouvelle durée doit être strictement supérieure à la durée actuelle.

            Si `nouvelleDate` est absent, seul le statut est mis à jour (ACTIF), sans modifier les dates.

            **Accès : ADMIN ou SUPER_ADMIN**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prolongation acceptée — statut : ACTIF",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Transition invalide, durée insuffisante ou identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Contrat non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}/prolongation/accepter")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RestResponse<ContratResponseDto>> accepterProlongation(
            @PathVariable Long id, @RequestBody(required = false) ContratActionDto dto) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de contrat invalide", null));
        }
        return ResponseEntity.ok(RestResponse.success(service.accepterProlongation(id, dto), HttpStatus.OK));
    }

    @Operation(
        summary = "Refuser une demande de prolongation",
        description = """
            L'administrateur refuse la demande de prolongation du client.

            Transition : `PROLONGATION_EN_ATTENTE` → `ACTIF`

            Un commentaire admin optionnel peut être fourni dans le corps de la requête (`motif`).

            **Accès : ADMIN ou SUPER_ADMIN**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prolongation refusée — statut : ACTIF",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Transition invalide ou identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Contrat non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}/prolongation/refuser")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RestResponse<ContratResponseDto>> refuserProlongation(
            @PathVariable Long id, @RequestBody(required = false) ContratActionDto dto) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de contrat invalide", null));
        }
        return ResponseEntity.ok(RestResponse.success(service.refuserProlongation(id, dto), HttpStatus.OK));
    }

    @Operation(summary = "Historique d'un contrat", description = "Retourne l'historique complet des transitions et actions sur un contrat. **Accès : ADMIN uniquement**")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historique du contrat",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Contrat non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}/historique")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<ContratHistoryDto>> getHistorique(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(PagedResponse.fromPage(historyService.getHistory(id, pageable)));
    }

    private boolean isAdmin(Principal principal) {
        var auth = (org.springframework.security.core.Authentication) principal;
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }
}
