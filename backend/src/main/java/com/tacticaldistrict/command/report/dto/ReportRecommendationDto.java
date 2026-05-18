package com.tacticaldistrict.command.report.dto;

import com.tacticaldistrict.command.report.model.ReportRecommendationSeverity;

public record ReportRecommendationDto(
        String code,
        ReportRecommendationSeverity severity,
        String title,
        String description,
        String actionLabel,
        String actionRoute
) {
}
