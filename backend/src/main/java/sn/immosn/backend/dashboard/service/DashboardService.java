package sn.immosn.backend.dashboard.service;

import sn.immosn.backend.client.web.dashboard.dto.DashboardStatsDto;
import sn.immosn.backend.client.web.dashboard.dto.RecentActivityDto;
import sn.immosn.backend.shared.response.PagedResponse;

public interface DashboardService {
    DashboardStatsDto getStats();
    PagedResponse<RecentActivityDto> getActivities(int page, int size, String type);
}
