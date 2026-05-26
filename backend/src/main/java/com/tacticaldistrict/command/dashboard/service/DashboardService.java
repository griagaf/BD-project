package com.tacticaldistrict.command.dashboard.service;

import com.tacticaldistrict.command.alert.dto.TacticalAlertDto;
import com.tacticaldistrict.command.alert.service.AlertService;
import com.tacticaldistrict.command.dashboard.application.port.DashboardRepositoryPort;
import com.tacticaldistrict.command.dashboard.dto.AuditEventDto;
import com.tacticaldistrict.command.dashboard.dto.DashboardStatisticsDto;
import com.tacticaldistrict.command.dashboard.dto.ProblemZoneDto;
import com.tacticaldistrict.command.dashboard.dto.ReadinessDto;
import com.tacticaldistrict.command.dashboard.dto.TacticalDashboardDto;
import com.tacticaldistrict.command.dashboard.domain.ProblemZoneAssembler;
import com.tacticaldistrict.command.dashboard.domain.ReadinessCalculator;
import com.tacticaldistrict.command.security.access.PermissionService;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int LATEST_EVENTS_LIMIT = 30;

    private final DashboardRepositoryPort dashboardRepository;
    private final UserContextProvider userContextProvider;
    private final PermissionService permissionService;
    private final AlertService alertService;
    private final ReadinessCalculator readinessCalculator;
    private final ProblemZoneAssembler problemZoneAssembler;

    @Transactional(readOnly = true)
    public TacticalDashboardDto dashboard() {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("dashboard:read")) {
            throw new AccessDeniedException("Access denied");
        }

        List<TacticalAlertDto> alerts = alertService.alerts();
        DashboardStatisticsDto statistics = statistics(user, alerts.size());
        ReadinessDto readiness = readinessCalculator.calculate(statistics, alerts);
        List<ProblemZoneDto> problemZones = problemZoneAssembler.assemble(alerts);
        List<TacticalAlertDto> criticalAlerts = alerts.stream()
                .filter(alert -> "CRITICAL".equals(alert.severity()) || "HIGH".equals(alert.severity()))
                .limit(5)
                .toList();

        return new TacticalDashboardDto(
                statistics,
                readiness,
                problemZones,
                latestEvents(user),
                criticalAlerts
        );
    }

    @Transactional(readOnly = true)
    public ReadinessDto readiness() {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("dashboard:read")) {
            throw new AccessDeniedException("Access denied");
        }
        List<TacticalAlertDto> alerts = alertService.alerts();
        return readinessCalculator.calculate(statistics(user, alerts.size()), alerts);
    }

    private DashboardStatisticsDto statistics(UserContext user, long alertCount) {
        List<Long> formationIds = dashboardRepository.formationIds();
        List<Long> unitIds = dashboardRepository.unitIds();
        List<Long> subdivisionIds = dashboardRepository.subdivisionIds();
        List<Long> personnelIds = dashboardRepository.personnelIds();
        List<Long> buildingIds = dashboardRepository.buildingIds();

        long formations = formationIds.stream()
                .filter(id -> permissionService.canRead(user, ObjectType.FORMATION, id)
                        || permissionService.canRead(user, ObjectType.DISTRICT, id)
                        || permissionService.canRead(user, ObjectType.ARMY, id)
                        || permissionService.canRead(user, ObjectType.BRIGADE, id))
                .count();
        long units = unitIds.stream().filter(id -> permissionService.canRead(user, ObjectType.MILITARY_UNIT, id)).count();
        long subdivisions = subdivisionIds.stream().filter(id -> canReadSubdivision(user, id)).count();
        long personnel = personnelIds.stream().filter(id -> permissionService.canRead(user, ObjectType.PERSONNEL, id)).count();
        long buildings = buildingIds.stream().filter(id -> permissionService.canRead(user, ObjectType.BUILDING, id)).count();
        long equipmentQuantity = inventoryQuantity(user, dashboardRepository.equipmentQuantities());
        long weaponQuantity = inventoryQuantity(user, dashboardRepository.weaponQuantities());

        return new DashboardStatisticsDto(
                formations,
                units,
                subdivisions,
                personnel,
                equipmentQuantity,
                weaponQuantity,
                buildings,
                alertCount
        );
    }

    private List<AuditEventDto> latestEvents(UserContext user) {
        return dashboardRepository.latestEvents(LATEST_EVENTS_LIMIT).stream()
                .filter(event -> event.objectId() == null || canReadAuditObject(user, event.objectType(), event.objectId()))
                .limit(8)
                .toList();
    }

    private long inventoryQuantity(UserContext user, List<DashboardRepositoryPort.InventoryQuantityRow> rows) {
        return rows.stream()
                .filter(row -> permissionService.canRead(user, ObjectType.MILITARY_UNIT, row.unitId()))
                .mapToLong(DashboardRepositoryPort.InventoryQuantityRow::quantity)
                .sum();
    }

    private boolean canReadSubdivision(UserContext user, Long id) {
        return permissionService.canRead(user, ObjectType.BATTALION, id)
                || permissionService.canRead(user, ObjectType.COMPANY, id)
                || permissionService.canRead(user, ObjectType.PLATOON, id)
                || permissionService.canRead(user, ObjectType.SQUAD, id);
    }

    private boolean canReadAuditObject(UserContext user, String objectType, Long objectId) {
        try {
            return permissionService.canRead(user, ObjectType.from(objectType), objectId);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

}
