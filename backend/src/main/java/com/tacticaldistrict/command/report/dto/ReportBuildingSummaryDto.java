package com.tacticaldistrict.command.report.dto;

import java.util.List;

public record ReportBuildingSummaryDto(
        long total,
        long unused,
        long overloaded,
        List<BuildingUsageDto> problemBuildings
) {
}
