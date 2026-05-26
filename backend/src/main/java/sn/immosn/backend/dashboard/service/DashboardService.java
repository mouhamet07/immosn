package sn.immosn.backend.dashboard.service;

import sn.immosn.backend.client.web.dashboard.dto.DashboardStatsDto;

public interface DashboardService {
    DashboardStatsDto getStats();
}
