package sn.immosn.backend.client.web.lead.controller;

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
import sn.immosn.backend.client.web.lead.dto.*;
import sn.immosn.backend.lead.data.entity.StatutLead;
import sn.immosn.backend.lead.service.LeadHistoryService;
import sn.immosn.backend.lead.service.LeadService;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "LEADS", description = "Gestion du pipeline commercial : qualification des prospects intéressés par une annonce, issus des visites")
@SecurityRequirement(name = "bearerAuth")
public class LeadController {

    private final LeadService service;
    private final LeadHistoryService historyService;

    @Operation(
        summary = "Créer un lead",
        description = """
            Crée un lead (prospect qualifié) pour un client qui a montré un intérêt sérieux
            pour une annonce.

            **Note :** dans le flux normal, les leads sont créés automatiquement dès qu'un
            client soumet une demande de visite — il n'est pas nécessaire de les créer
            manuellement. Cet endpoint permet des créations hors flux visite.

            Le lead peut être lié à une demande de visite et sert d'étape intermédiaire
            avant la création d'un contrat.

            Statuts :
            - `EN_COURS` → `ABANDONNE` : abandon manuel (toujours permis)
            - `EN_COURS` → `CONVERTI` : **uniquement si le lead n'est PAS lié à une visite**.
              Si le lead a une visite associée, la conversion passe obligatoirement par
              `PUT /api/v1/visites/{id}/cloture` avec `type=AVEC_CONTRAT`.

            **Accès : ADMIN ou SUPER_ADMIN**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Lead créé avec succès",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 201,
                      "data": {
                        "id": 3,
                        "clientNom": "Aminata Diallo",
                        "clientEmail": "aminata@immosn.sn",
                        "annonceLibelle": "Villa F5 - Almadies",
                        "annonceAdresse": "Route des Almadies",
                        "visiteId": 12,
                        "statut": "EN_COURS",
                        "noteAdmin": "Cliente très intéressée, budget confirmé",
                        "createdAt": "2024-01-22T10:00:00"
                      }
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Données invalides — client ou annonce manquant",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Client, annonce ou visite non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping
    public ResponseEntity<RestResponse<LeadResponseDto>> create(@RequestBody @Valid LeadCreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RestResponse.success(service.create(request), HttpStatus.CREATED));
    }

    @Operation(
        summary = "Lister tous les leads",
        description = """
            Retourne la liste paginée de tous les leads, triée par date de création décroissante.

            Peut être filtrée par statut pour suivre le pipeline commercial :
            `EN_COURS` | `CONVERTI` | `ABANDONNE`

            **Accès : ADMIN ou SUPER_ADMIN**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des leads",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping
    public ResponseEntity<PagedResponse<LeadResponseDto>> getAll(
            @Parameter(description = "Numéro de page", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filtre optionnel par statut du lead") @RequestParam(required = false) StatutLead statut) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PagedResponse.fromPage(service.getAll(statut, pageable)));
    }

    @Operation(
        summary = "Détail d'un lead",
        description = """
            Retourne les informations complètes d'un lead spécifique.

            **Accès : ADMIN ou SUPER_ADMIN**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Détail du lead",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 200,
                      "data": {
                        "id": 3,
                        "clientId": 42,
                        "clientNom": "Aminata Diallo",
                        "clientEmail": "aminata@immosn.sn",
                        "annonceId": 15,
                        "annonceLibelle": "Villa F5 - Almadies",
                        "visiteId": 12,
                        "statut": "EN_COURS",
                        "noteAdmin": "Cliente très intéressée, budget confirmé",
                        "createdAt": "2024-01-22T10:00:00",
                        "updatedAt": "2024-01-22T10:00:00"
                      }
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Lead non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<LeadResponseDto>> getById(
            @Parameter(description = "Identifiant du lead", required = true, example = "3")
            @PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de lead invalide", null));
        }
        return ResponseEntity.ok(RestResponse.success(service.getById(id), HttpStatus.OK));
    }

    @Operation(
        summary = "Mettre à jour le statut d'un lead",
        description = """
            Change le statut d'un lead dans le pipeline commercial.

            **Règles métier :**
            - Un lead **sans visite associée** peut être manuellement passé à `CONVERTI` ou `ABANDONNE`.
            - Un lead **avec visite associée** est en **lecture seule** pour cet endpoint.
              Les transitions de statut sont déclenchées automatiquement par la visite :
              - `CONVERTI` ← `PUT /api/v1/visites/{id}/cloture` avec `type=AVEC_CONTRAT`
              - `ABANDONNE` ← `PUT /api/v1/visites/{id}/cloture` avec `type=SANS_SUITE`
              - `ABANDONNE` ← `PUT /api/v1/visites/{id}/status` (REFUSEE ou ANNULEE)

            Toute tentative de modification de statut sur un lead lié à une visite retourne **HTTP 422**.

            Une note admin peut être mise à jour en même temps pour documenter la décision.

            **Accès : ADMIN ou SUPER_ADMIN**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statut du lead mis à jour",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {"success":true,"status":200,"data":{"id":3,"statut":"CONVERTI","noteAdmin":"Contrat signé le 25/01/2024"}}"""))),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide ou transition de statut non autorisée",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Lead non trouvé",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "422", description = "Règle métier violée — le lead est lié à une visite et ne peut être modifié que depuis celle-ci",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<RestResponse<LeadResponseDto>> updateStatut(
            @Parameter(description = "Identifiant du lead", required = true, example = "3")
            @PathVariable Long id,
            @RequestBody @Valid UpdateStatutLeadDto dto) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de lead invalide", null));
        }
        return ResponseEntity.ok(RestResponse.success(service.updateStatut(id, dto), HttpStatus.OK));
    }

    @Operation(
        summary = "Historique d'un lead",
        description = "Retourne l'historique complet des transitions et actions sur un lead. **Accès : ADMIN ou SUPER_ADMIN**"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historique du lead",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Lead non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}/historique")
    public ResponseEntity<PagedResponse<LeadHistoryDto>> getHistorique(
            @Parameter(description = "Identifiant du lead", required = true) @PathVariable Long id,
            @Parameter(description = "Numéro de page", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page", example = "20") @RequestParam(defaultValue = "20") int size) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(PagedResponse.fromPage(historyService.getHistory(id, pageable)));
    }

    @Operation(
        summary = "Mettre à jour la note admin d'un lead",
        description = """
            Met à jour uniquement la note administrative d'un lead sans modifier son statut.

            La note est indépendante du statut — elle peut être modifiée à tout moment,
            y compris sur les leads CONVERTI ou ABANDONNE.

            **Accès : ADMIN ou SUPER_ADMIN**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Note mise à jour",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Note trop longue (max 2 000 caractères)",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Lead non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}/note")
    public ResponseEntity<RestResponse<LeadResponseDto>> updateNote(
            @Parameter(description = "Identifiant du lead", required = true, example = "3")
            @PathVariable Long id,
            @RequestBody @Valid UpdateNoteLeadDto dto) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de lead invalide", null));
        }
        return ResponseEntity.ok(RestResponse.success(service.updateNote(id, dto), HttpStatus.OK));
    }
}
