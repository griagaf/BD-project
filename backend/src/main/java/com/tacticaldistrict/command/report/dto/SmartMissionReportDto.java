package com.tacticaldistrict.command.report.dto;

import java.time.Instant;
import java.util.List;

public record SmartMissionReportDto(
        String reportId,
        ReportObjectDto object,
        Instant generatedAt,
        List<ReportCommanderDto> commanders,
        ReportPersonnelSummaryDto personnel,
        ReportEquipmentSummaryDto equipment,
        ReportWeaponSummaryDto weapons,
        ReportBuildingSummaryDto buildings,
        ReportSpecialtySummaryDto specialties,
        List<ReportAlertDto> alerts,
        ReportReadinessDto readiness,
        List<ReportRecommendationDto> recommendations
) {
}
