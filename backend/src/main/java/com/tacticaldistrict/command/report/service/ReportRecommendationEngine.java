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
                    "Готовность ниже оперативного порога",
                    "Проверьте дефицит ресурсов, предупреждения и укомплектованность перед постановкой задачи.",
                    "Открыть предупреждения", "/alerts"));
        }
        if (equipment.unitsWithoutEquipment() > 0) {
            result.add(recommendation("UNITS_WITHOUT_EQUIPMENT", ReportRecommendationSeverity.HIGH,
                    "Обнаружены части без техники",
                    "Назначьте технику или проверьте полноту инвентарного реестра.",
                    "Открыть запрос по технике", "/intelligence"));
        }
        if (weapons.unitsWithoutWeapons() > 0) {
            result.add(recommendation("UNITS_WITHOUT_WEAPONS", ReportRecommendationSeverity.HIGH,
                    "Обнаружены части без вооружения",
                    "Проверьте распределение вооружения по затронутым частям.",
                    "Открыть запрос по вооружению", "/intelligence"));
        }
        if (specialties.missingSpecialties() > 0) {
            result.add(recommendation("MISSING_SPECIALISTS", ReportRecommendationSeverity.MEDIUM,
                    "Покрытие специальностей неполное",
                    "Проверьте отсутствующие специальности и назначения личного состава.",
                    "Открыть личный состав", "/personnel"));
        }
        if (buildings.overloaded() > 0) {
            result.add(recommendation("OVERLOADED_BUILDINGS", ReportRecommendationSeverity.MEDIUM,
                    "Обнаружены перегруженные сооружения",
                    "Проверьте закрепление сооружений и перераспределите подразделения.",
                    "Открыть сооружения", "/buildings"));
        }
        if (personnel.total() == 0) {
            result.add(recommendation("NO_PERSONNEL", ReportRecommendationSeverity.CRITICAL,
                    "В выбранной области нет личного состава",
                    "Проверьте иерархию назначений и полноту реестра военнослужащих.",
                    "Открыть иерархию", "/hierarchy"));
        }
        long criticalAlerts = alerts.stream().filter(alert -> "CRITICAL".equals(alert.severity())).count();
        if (criticalAlerts > 0) {
            result.add(recommendation("CRITICAL_ALERTS_PRESENT", ReportRecommendationSeverity.CRITICAL,
                    "Критические предупреждения требуют внимания",
                    "Устраните критические предупреждения перед подтверждением готовности.",
                    "Открыть критические предупреждения", "/alerts"));
        }
        if (result.isEmpty()) {
            result.add(recommendation("MISSION_READY", ReportRecommendationSeverity.INFO,
                    "Блокирующих проблем не обнаружено",
                    "Выбранная область имеет достаточную базовую готовность для планирования задачи.",
                    "Открыть панель управления", "/dashboard"));
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
