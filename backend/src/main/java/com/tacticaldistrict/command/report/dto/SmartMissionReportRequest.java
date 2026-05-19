package com.tacticaldistrict.command.report.dto;

import com.tacticaldistrict.command.report.model.ReportObjectType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SmartMissionReportRequest(
        @NotNull ReportObjectType objectType,
        @NotNull @Positive Long objectId,
        Boolean includePersonnel,
        Boolean includeResources,
        Boolean includeAlerts,
        Boolean includeRecommendations
) {
    public SmartMissionReportRequest {
        includePersonnel = includePersonnel == null || includePersonnel;
        includeResources = includeResources == null || includeResources;
        includeAlerts = includeAlerts == null || includeAlerts;
        includeRecommendations = includeRecommendations == null || includeRecommendations;
    }
}
