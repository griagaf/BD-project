package com.tacticaldistrict.command.report.dto;

import com.tacticaldistrict.command.report.model.ReportObjectType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SmartMissionReportRequest(
        @NotNull ReportObjectType objectType,
        @NotNull @Positive Long objectId,
        boolean includePersonnel,
        boolean includeResources,
        boolean includeAlerts,
        boolean includeRecommendations
) {
}
