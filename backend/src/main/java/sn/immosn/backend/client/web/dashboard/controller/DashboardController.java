package sn.immosn.backend.client.web.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.immosn.backend.client.web.dashboard.dto.DashboardStatsDto;
import sn.immosn.backend.dashboard.service.DashboardService;
import sn.immosn.backend.shared.response.RestResponse;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/v1/admin/dashboard/stats
     * Retourne toutes les statistiques globales en un seul appel.
     */
    @GetMapping("/stats")
    public ResponseEntity<RestResponse<DashboardStatsDto>> getStats() {
        return ResponseEntity.ok(
            RestResponse.success(dashboardService.getStats(), HttpStatus.OK)
        );
    }
}
