package com.tacticaldistrict.command.report.service;

import com.tacticaldistrict.command.report.dto.ReportAlertDto;
import com.tacticaldistrict.command.report.dto.ReportBuildingSummaryDto;
import com.tacticaldistrict.command.report.dto.ReportEquipmentSummaryDto;
import com.tacticaldistrict.command.report.dto.ReportPersonnelSummaryDto;
import com.tacticaldistrict.command.report.dto.ReportReadinessDto;
import com.tacticaldistrict.command.report.dto.ReportRecommendationDto;
import com.tacticaldistrict.command.report.dto.ReportSpecialtySummaryDto;
import com.tacticaldistrict.command.report.dto.ReportWeaponSummaryDto;
import com.tacticaldistrict.command.report.model.ReportRecommendationSeverity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReportRecommendationEngine {

    public List<ReportRecommendationDto> generate(
            ReportPersonnelSummaryDto personnel,
            ReportEquipmentSummaryDto equipment,
            ReportWeaponSummaryDto weapons,
            ReportBuildingSummaryDto buildings,
            ReportSpecialtySummaryDto specialties,
            List<ReportAlertDto> alerts,
            ReportReadinessDto readiness
    ) {
        List<ReportRecommendationDto> result = new ArrayList<>();
        if (readiness.overall() < 60) {
            result.add(recommendation("LOW_OVERALL_READINESS", ReportRecommendationSeverity.CRITICAL,
                    "Overall readiness is below operational threshold",
                    "Review resource gaps, alerts and personnel coverage before mission assignment.",
                    "Open alerts", "/alerts"));
        }
        if (equipment.unitsWithoutEquipment() > 0) {
            result.add(recommendation("UNITS_WITHOUT_EQUIPMENT", ReportRecommendationSeverity.HIGH,
                    "Units without equipment detected",
                    "Assign equipment or verify equipment registry completeness.",
                    "Open equipment query", "/intelligence"));
        }
        if (weapons.unitsWithoutWeapons() > 0) {
            result.add(recommendation("UNITS_WITHOUT_WEAPONS", ReportRecommendationSeverity.HIGH,
                    "Units without weapons detected",
                    "Review weapon distribution for affected units.",
                    "Open weapons query", "/intelligence"));
        }
        if (specialties.missingSpecialties() > 0) {
            result.add(recommendation("MISSING_SPECIALISTS", ReportRecommendationSeverity.MEDIUM,
                    "Specialty coverage is incomplete",
                    "Check missing specialties and personnel assignment.",
                    "Open personnel", "/personnel"));
        }
        if (buildings.overloaded() > 0) {
            result.add(recommendation("OVERLOADED_BUILDINGS", ReportRecommendationSeverity.MEDIUM,
                    "Overloaded buildings detected",
                    "Review building assignment and redistribute subdivisions.",
                    "Open buildings", "/buildings"));
        }
        if (personnel.total() == 0) {
            result.add(recommendation("NO_PERSONNEL", ReportRecommendationSeverity.CRITICAL,
                    "No personnel in selected scope",
                    "Verify hierarchy assignment and personnel registry completeness.",
                    "Open hierarchy", "/hierarchy"));
        }
        long criticalAlerts = alerts.stream().filter(alert -> "CRITICAL".equals(alert.severity())).count();
        if (criticalAlerts > 0) {
            result.add(recommendation("CRITICAL_ALERTS_PRESENT", ReportRecommendationSeverity.CRITICAL,
                    "Critical alerts require attention",
                    "Resolve critical alerts before confirming operational readiness.",
                    "Open critical alerts", "/alerts"));
        }
        if (result.isEmpty()) {
            result.add(recommendation("MISSION_READY", ReportRecommendationSeverity.INFO,
                    "No blocking findings detected",
                    "Selected scope has sufficient baseline readiness for mission planning.",
                    "Open dashboard", "/dashboard"));
        }
        return result;
    }

    private ReportRecommendationDto recommendation(
            String code,
            ReportRecommendationSeverity severity,
            String title,
            String description,
            String actionLabel,
            String actionRoute
    ) {
        return new ReportRecommendationDto(code, severity, title, description, actionLabel, actionRoute);
    }
}
