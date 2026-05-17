package com.tacticaldistrict.command.dashboard.service;

import com.tacticaldistrict.command.alert.dto.TacticalAlertDto;
import com.tacticaldistrict.command.alert.service.AlertService;
import com.tacticaldistrict.command.dashboard.dto.AuditEventDto;
import com.tacticaldistrict.command.dashboard.dto.DashboardStatisticsDto;
import com.tacticaldistrict.command.dashboard.dto.ProblemZoneDto;
import com.tacticaldistrict.command.dashboard.dto.ReadinessAxisDto;
import com.tacticaldistrict.command.dashboard.dto.ReadinessDto;
import com.tacticaldistrict.command.dashboard.dto.TacticalDashboardDto;
import com.tacticaldistrict.command.security.access.PermissionService;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final UserContextProvider userContextProvider;
    private final PermissionService permissionService;
    private final AlertService alertService;

    @Transactional(readOnly = true)
    public TacticalDashboardDto dashboard() {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("dashboard:read")) {
            throw new AccessDeniedException("Access denied");
        }

        List<TacticalAlertDto> alerts = alertService.alerts();
        DashboardStatisticsDto statistics = statistics(user, alerts.size());
        ReadinessDto readiness = readiness(statistics, alerts);
        List<ProblemZoneDto> problemZones = problemZones(alerts);
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
        return readiness(statistics(user, alerts.size()), alerts);
    }

    private DashboardStatisticsDto statistics(UserContext user, long alertCount) {
        List<Long> formationIds = ids("SELECT formation_id FROM military_formations");
        List<Long> unitIds = ids("SELECT unit_id FROM military_units");
        List<Long> subdivisionIds = ids("SELECT subdivision_id FROM subdivisions");
        List<Long> personnelIds = ids("SELECT personnel_id FROM personnel");
        List<Long> buildingIds = ids("SELECT building_id FROM buildings");

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
        long equipmentQuantity = inventoryQuantity(user, "equipment_in_units");
        long weaponQuantity = inventoryQuantity(user, "weapon_in_units");

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

    private ReadinessDto readiness(DashboardStatisticsDto stats, List<TacticalAlertDto> alerts) {
        long unitsWithoutEquipment = count(alerts, "UNIT_WITHOUT_EQUIPMENT");
        long unitsWithoutWeapons = count(alerts, "UNIT_WITHOUT_WEAPONS");
        long specialtyGaps = count(alerts, "SPECIALTY_WITHOUT_SPECIALISTS");
        long infraAlerts = count(alerts, "BUILDING_WITHOUT_SUBDIVISIONS") + count(alerts, "BUILDING_OVERLOADED");
        long critical = alerts.stream().filter(alert -> "CRITICAL".equals(alert.severity())).count();

        int personnel = clamp(stats.personnel() == 0 ? 0 : 85 - (int) Math.min(30, specialtyGaps * 4));
        int equipment = clamp(stats.units() == 0 ? 0 : 100 - (int) Math.min(70, unitsWithoutEquipment * 25 + critical * 5));
        int weapons = clamp(stats.units() == 0 ? 0 : 100 - (int) Math.min(70, unitsWithoutWeapons * 25 + critical * 5));
        int specialists = clamp(90 - (int) Math.min(60, specialtyGaps * 12));
        int infrastructure = clamp(stats.buildings() == 0 ? 0 : 92 - (int) Math.min(60, infraAlerts * 10));
        int overall = clamp((personnel + equipment + weapons + specialists + infrastructure) / 5);

        return new ReadinessDto(overall, List.of(
                axis("personnel", "Личный состав", personnel),
                axis("equipment", "Техника", equipment),
                axis("weapons", "Вооружение", weapons),
                axis("specialists", "Специалисты", specialists),
                axis("infrastructure", "Инфраструктура", infrastructure)
        ));
    }

    private List<ProblemZoneDto> problemZones(List<TacticalAlertDto> alerts) {
        return alerts.stream()
                .collect(Collectors.groupingBy(TacticalAlertDto::type, java.util.LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> new ProblemZoneDto(
                        entry.getKey(),
                        label(entry.getKey()),
                        entry.getValue().stream().map(TacticalAlertDto::severity).findFirst().orElse("LOW"),
                        (long) entry.getValue().size()
                ))
                .toList();
    }

    private List<AuditEventDto> latestEvents(UserContext user) {
        return jdbcTemplate.query("""
                SELECT audit_event_id, actor_username, action, object_type, object_id, details, created_at
                FROM audit_events
                ORDER BY created_at DESC, audit_event_id DESC
                LIMIT 30
                """, Map.of(), (rs, rowNum) -> {
            Timestamp timestamp = rs.getTimestamp("created_at");
            return new AuditEventDto(
                    rs.getLong("audit_event_id"),
                    rs.getString("action"),
                    rs.getString("object_type"),
                    rs.getObject("object_id", Long.class),
                    rs.getString("actor_username"),
                    rs.getString("details"),
                    timestamp == null ? Instant.now() : timestamp.toInstant()
            );
        }).stream()
                .filter(event -> event.objectId() == null || canReadAuditObject(user, event.objectType(), event.objectId()))
                .limit(8)
                .toList();
    }

    private long inventoryQuantity(UserContext user, String table) {
        return jdbcTemplate.queryForList("SELECT unit_id, quantity FROM " + table, Map.of())
                .stream()
                .filter(row -> permissionService.canRead(user, ObjectType.MILITARY_UNIT, ((Number) row.get("unit_id")).longValue()))
                .mapToLong(row -> ((Number) row.get("quantity")).longValue())
                .sum();
    }

    private List<Long> ids(String sql) {
        return jdbcTemplate.query(sql, Map.of(), (rs, rowNum) -> rs.getLong(1));
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

    private long count(List<TacticalAlertDto> alerts, String type) {
        return alerts.stream().filter(alert -> type.equals(alert.type())).count();
    }

    private ReadinessAxisDto axis(String key, String label, int score) {
        return new ReadinessAxisDto(key, label, score, score >= 80 ? "READY" : score >= 55 ? "WATCH" : "CRITICAL");
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String label(String type) {
        return switch (type) {
            case "UNIT_WITHOUT_EQUIPMENT" -> "Части без техники";
            case "UNIT_WITHOUT_WEAPONS" -> "Части без вооружения";
            case "BUILDING_WITHOUT_SUBDIVISIONS" -> "Свободные сооружения";
            case "BUILDING_OVERLOADED" -> "Перегруженные сооружения";
            case "SPECIALTY_WITHOUT_SPECIALISTS" -> "Специальности без специалистов";
            case "EQUIPMENT_QUANTITY_EXCEEDED" -> "Превышение количества техники";
            case "WEAPON_QUANTITY_EXCEEDED" -> "Превышение количества вооружения";
            default -> type;
        };
    }
}
