package sn.immosn.backend.client.web.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.immosn.backend.client.web.dashboard.dto.DashboardStatsDto;
import sn.immosn.backend.client.web.dashboard.dto.RecentActivityDto;
import sn.immosn.backend.dashboard.service.DashboardService;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "ADMINISTRATION", description = "Tableau de bord d'administration — statistiques globales et activités récentes de la plateforme")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
        summary = "Statistiques globales du tableau de bord",
        description = """
            Retourne toutes les statistiques de la plateforme en un seul appel optimisé.

            Inclut :
            - Compteurs globaux : annonces, clients, admins, contrats, visites, signalements, leads, discussions
            - Compteurs filtrés : annonces actives, contrats actifs, visites en attente, visites du jour,
              signalements ouverts, leads en cours
            - Liste des activités récentes (créations, changements de statut)

            Cet endpoint est appelé à l'initialisation du tableau de bord administrateur.

            **Accès : ADMIN ou SUPER_ADMIN**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistiques globales retournées avec succès",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 200,
                      "data": {
                        "totalAnnonces": 48,
                        "annoncesActives": 42,
                        "totalClients": 156,
                        "totalAdmins": 3,
                        "totalContrats": 27,
                        "contratsActifs": 19,
                        "totalVisites": 84,
                        "visitesEnAttente": 7,
                        "visitesAujourdhui": 2,
                        "totalSignalements": 12,
                        "signalementsOuverts": 3,
                        "totalLeads": 35,
                        "leadsEnCours": 8,
                        "totalDiscussions": 93,
                        "activitesRecentes": [
                          {
                            "type": "VISITE",
                            "titre": "Nouvelle demande de visite",
                            "description": "Aminata Diallo - Villa F5 Almadies",
                            "statut": "EN_ATTENTE",
                            "createdAt": "2024-01-15T11:30:00"
                          },
                          {
                            "type": "SIGNALEMENT",
                            "titre": "Nouveau signalement",
                            "description": "Moussa Ndiaye - Contrat #8",
                            "statut": "OUVERT",
                            "createdAt": "2024-01-15T10:15:00"
                          }
                        ]
                      }
                    }"""))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle ADMIN ou SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Erreur interne lors du calcul des statistiques",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/stats")
    public ResponseEntity<RestResponse<DashboardStatsDto>> getStats() {
        return ResponseEntity.ok(
            RestResponse.success(dashboardService.getStats(), HttpStatus.OK)
        );
    }

    @Operation(
        summary = "Activités récentes paginées et filtrées",
        description = """
            Retourne les activités récentes paginées avec priorité aux items en attente d'action.

            Tri appliqué : EN_ATTENTE / OUVERT en premier, puis createdAt DESC dans chaque groupe.

            Filtres `type` supportés : ALL | VISITE | CONTRAT | SIGNALEMENT | BIEN | MESSAGE

            **Accès : ADMIN ou SUPER_ADMIN**
            """
    )
    @GetMapping("/activities")
    public ResponseEntity<RestResponse<PagedResponse<RecentActivityDto>>> getActivities(
            @RequestParam(defaultValue = "0")   int    page,
            @RequestParam(defaultValue = "5")   int    size,
            @RequestParam(defaultValue = "ALL") String type) {
        return ResponseEntity.ok(
            RestResponse.success(dashboardService.getActivities(page, size, type), HttpStatus.OK)
        );
    }
}
