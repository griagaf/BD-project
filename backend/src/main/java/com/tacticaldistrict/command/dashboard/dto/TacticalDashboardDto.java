package com.tacticaldistrict.command.dashboard.dto;

import com.tacticaldistrict.command.alert.dto.TacticalAlertDto;
import java.util.List;

public record TacticalDashboardDto(
        DashboardStatisticsDto statistics,
        ReadinessDto readiness,
        List<ProblemZoneDto> problemZones,
        List<AuditEventDto> latestEvents,
        List<TacticalAlertDto> criticalAlerts
) {
}
