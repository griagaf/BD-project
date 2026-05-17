package com.tacticaldistrict.command.report.service;

import com.tacticaldistrict.command.alert.dto.TacticalAlertDto;
import com.tacticaldistrict.command.alert.service.AlertService;
import com.tacticaldistrict.command.report.dto.ReportAlertDto;
import com.tacticaldistrict.command.report.dto.ReportBuildingSummaryDto;
import com.tacticaldistrict.command.report.dto.ReportEquipmentSummaryDto;
import com.tacticaldistrict.command.report.dto.ReportPersonnelSummaryDto;
import com.tacticaldistrict.command.report.dto.ReportReadinessDto;
import com.tacticaldistrict.command.report.dto.ReportRecommendationDto;
import com.tacticaldistrict.command.report.dto.ReportSpecialtySummaryDto;
import com.tacticaldistrict.command.report.dto.ReportWeaponSummaryDto;
import com.tacticaldistrict.command.report.dto.SmartMissionReportDto;
import com.tacticaldistrict.command.report.dto.SmartMissionReportRequest;
import com.tacticaldistrict.command.report.repository.ReportDataRepository;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.user.service.UserContext;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportGenerator {

    private final ReportDataRepository reportDataRepository;
    private final AlertService alertService;
    private final ReportRecommendationEngine recommendationEngine;

    public SmartMissionReportDto generate(UserContext user, SmartMissionReportRequest request) {
        List<Long> unitIds = reportDataRepository.unitIds(request.objectType(), request.objectId());
        ReportPersonnelSummaryDto personnel = request.includePersonnel()
                ? reportDataRepository.personnelSummary(request.objectType(), request.objectId())
                : new ReportPersonnelSummaryDto(0, 0, 0, 0, java.util.Map.of(), java.util.Map.of());
        ReportEquipmentSummaryDto equipment = request.includeResources()
                ? reportDataRepository.equipmentSummary(unitIds)
                : new ReportEquipmentSummaryDto(0, 0, 0, List.of(), List.of());
        ReportWeaponSummaryDto weapons = request.includeResources()
                ? reportDataRepository.weaponSummary(unitIds)
                : new ReportWeaponSummaryDto(0, 0, 0, List.of(), List.of());
        ReportBuildingSummaryDto buildings = request.includeResources()
                ? reportDataRepository.buildingSummary(unitIds)
                : new ReportBuildingSummaryDto(0, 0, 0, List.of());
        ReportSpecialtySummaryDto specialties = request.includePersonnel()
                ? reportDataRepository.specialtySummary(request.objectType(), request.objectId())
                : new ReportSpecialtySummaryDto(0, 0, 0, List.of(), java.util.Map.of());
        List<ReportAlertDto> alerts = request.includeAlerts()
                ? scopedAlerts(unitIds)
                : List.of();
        ReportReadinessDto readiness = readiness(personnel, equipment, weapons, buildings, specialties, alerts);
        List<ReportRecommendationDto> recommendations = request.includeRecommendations()
                ? recommendationEngine.generate(personnel, equipment, weapons, buildings, specialties, alerts, readiness)
                : List.of();

        return new SmartMissionReportDto(
                UUID.randomUUID().toString(),
                reportDataRepository.objectInfo(request.objectType(), request.objectId()),
                Instant.now(),
                reportDataRepository.commanders(request.objectType(), request.objectId()),
                personnel,
                equipment,
                weapons,
                buildings,
                specialties,
                alerts,
                readiness,
                recommendations
        );
    }

    private List<ReportAlertDto> scopedAlerts(List<Long> unitIds) {
        if (unitIds.isEmpty()) {
            return List.of();
        }
        return alertService.alerts()
                .stream()
                .filter(alert -> alertBelongsToUnits(alert, unitIds))
                .map(alert -> new ReportAlertDto(
                        alert.id(),
                        alert.type(),
                        alert.severity(),
                        alert.title(),
                        alert.message(),
                        alert.objectType(),
                        alert.objectId()
                ))
                .toList();
    }

    private boolean alertBelongsToUnits(TacticalAlertDto alert, List<Long> unitIds) {
        if (ObjectType.MILITARY_UNIT.name().equals(alert.objectType())) {
            return unitIds.contains(alert.objectId());
        }
        if (ObjectType.BUILDING.name().equals(alert.objectType())) {
            Long unitId = reportDataRepository.buildingUnitId(alert.objectId());
            return unitId != null && unitIds.contains(unitId);
        }
        return ObjectType.SPECIALTY.name().equals(alert.objectType());
    }

    private ReportReadinessDto readiness(
            ReportPersonnelSummaryDto personnel,
            ReportEquipmentSummaryDto equipment,
            ReportWeaponSummaryDto weapons,
            ReportBuildingSummaryDto buildings,
            ReportSpecialtySummaryDto specialties,
            List<ReportAlertDto> alerts
    ) {
        int personnelScore = clamp(personnel.total() == 0 ? 0 : 88 - (int) Math.min(30, specialties.missingSpecialties() * 3));
        int equipmentScore = clamp(equipment.typesCount() == 0 ? 0 : 95 - (int) Math.min(70, equipment.unitsWithoutEquipment() * 25));
        int weaponScore = clamp(weapons.typesCount() == 0 ? 0 : 95 - (int) Math.min(70, weapons.unitsWithoutWeapons() * 25));
        int specialistScore = clamp(92 - (int) Math.min(60, specialties.missingSpecialties() * 8));
        int infrastructureScore = clamp(buildings.total() == 0 ? 0 : 92 - (int) Math.min(60, buildings.unused() * 8 + buildings.overloaded() * 10));
        long criticalAlerts = alerts.stream().filter(alert -> "CRITICAL".equals(alert.severity())).count();
        int overall = clamp((personnelScore + equipmentScore + weaponScore + specialistScore + infrastructureScore) / 5 - (int) criticalAlerts * 5);
        String status = overall >= 80 ? "OPERATIONAL" : overall >= 60 ? "OPERATIONAL_WITH_WARNINGS" : "LIMITED";
        return new ReportReadinessDto(overall, personnelScore, equipmentScore, weaponScore, specialistScore, infrastructureScore, status);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
