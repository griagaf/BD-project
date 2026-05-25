package com.tacticaldistrict.command.alert.service;

import com.tacticaldistrict.command.alert.dto.AlertActionDto;
import com.tacticaldistrict.command.alert.dto.TacticalAlertDto;
import com.tacticaldistrict.command.security.access.PermissionService;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertService {

    private static final int BUILDING_OVERLOAD_THRESHOLD = 3;
    private static final int EQUIPMENT_EXCEEDED_THRESHOLD = 300;
    private static final int WEAPON_EXCEEDED_THRESHOLD = 1000;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final UserContextProvider userContextProvider;
    private final PermissionService permissionService;

    @Transactional(readOnly = true)
    public List<TacticalAlertDto> alerts() {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("alert:read")) {
            throw new AccessDeniedException("Access denied");
        }

        List<TacticalAlertDto> result = new ArrayList<>();
        result.addAll(unitsWithoutEquipment(user));
        result.addAll(unitsWithoutWeapons(user));
        result.addAll(buildingsWithoutSubdivisions(user));
        result.addAll(overloadedBuildings(user));
        result.addAll(specialtiesWithoutSpecialists(user));
        result.addAll(equipmentQuantityExceeded(user));
        result.addAll(weaponQuantityExceeded(user));
        return result;
    }

    private List<TacticalAlertDto> unitsWithoutEquipment(UserContext user) {
        return jdbcTemplate.query("""
                SELECT mu.unit_id, mu.name AS unit_name
                FROM military_units mu
                WHERE NOT EXISTS (
                    SELECT 1 FROM equipment_in_units eiu WHERE eiu.unit_id = mu.unit_id
                )
                ORDER BY mu.name
                """, Map.of(), (rs, rowNum) -> alert(
                "UNIT_WITHOUT_EQUIPMENT",
                "HIGH",
                "В части отсутствует техника",
                rs.getString("unit_name") + ": техника не зарегистрирована в инвентаре.",
                ObjectType.MILITARY_UNIT,
                rs.getLong("unit_id"),
                Map.of("unitName", rs.getString("unit_name")),
                List.of(
                        new AlertActionDto("Открыть часть", "/units/" + rs.getLong("unit_id"), null),
                        new AlertActionDto("Добавить технику", "/equipment?unitId=" + rs.getLong("unit_id") + "&action=add", null),
                        new AlertActionDto("Показать технику", "/equipment?unitId=" + rs.getLong("unit_id"), "FIND_UNIT_EQUIPMENT"),
                        new AlertActionDto("Открыть терминал", "/intelligence", "FIND_EQUIPMENT_AVAILABILITY")
                )
        )).stream().filter(alert -> canRead(user, alert)).toList();
    }

    private List<TacticalAlertDto> unitsWithoutWeapons(UserContext user) {
        return jdbcTemplate.query("""
                SELECT mu.unit_id, mu.name AS unit_name
                FROM military_units mu
                WHERE NOT EXISTS (
                    SELECT 1 FROM weapon_in_units wiu WHERE wiu.unit_id = mu.unit_id
                )
                ORDER BY mu.name
                """, Map.of(), (rs, rowNum) -> alert(
                "UNIT_WITHOUT_WEAPONS",
                "HIGH",
                "В части отсутствует вооружение",
                rs.getString("unit_name") + ": вооружение не зарегистрировано в инвентаре.",
                ObjectType.MILITARY_UNIT,
                rs.getLong("unit_id"),
                Map.of("unitName", rs.getString("unit_name")),
                List.of(
                        new AlertActionDto("Открыть часть", "/units/" + rs.getLong("unit_id"), null),
                        new AlertActionDto("Добавить вооружение", "/weapons?unitId=" + rs.getLong("unit_id") + "&action=add", null),
                        new AlertActionDto("Показать вооружение", "/weapons?unitId=" + rs.getLong("unit_id"), "FIND_UNIT_WEAPONS"),
                        new AlertActionDto("Открыть терминал", "/intelligence", "FIND_WEAPON_AVAILABILITY")
                )
        )).stream().filter(alert -> canRead(user, alert)).toList();
    }

    private List<TacticalAlertDto> buildingsWithoutSubdivisions(UserContext user) {
        return jdbcTemplate.query("""
                SELECT b.building_id, b.name AS building_name, mu.name AS unit_name
                FROM buildings b
                JOIN military_units mu ON mu.unit_id = b.unit_id
                LEFT JOIN subdivision_buildings sb ON sb.building_id = b.building_id
                WHERE b.assignable = TRUE
                GROUP BY b.building_id, b.name, mu.name
                HAVING COUNT(sb.subdivision_id) = 0
                ORDER BY mu.name, b.name
                """, Map.of(), (rs, rowNum) -> alert(
                "BUILDING_WITHOUT_SUBDIVISIONS",
                "MEDIUM",
                "Сооружение не используется",
                rs.getString("building_name") + " не закреплено ни за одним подразделением.",
                ObjectType.BUILDING,
                rs.getLong("building_id"),
                Map.of("buildingName", rs.getString("building_name"), "unitName", rs.getString("unit_name")),
                List.of(
                        new AlertActionDto("Открыть сооружения", "/buildings", null),
                        new AlertActionDto("Открыть терминал", "/intelligence", "FIND_BUILDING_USAGE")
                )
        )).stream().filter(alert -> canRead(user, alert)).toList();
    }

    private List<TacticalAlertDto> overloadedBuildings(UserContext user) {
        return jdbcTemplate.query("""
                SELECT b.building_id, b.name AS building_name, mu.name AS unit_name, COUNT(sb.subdivision_id) AS subdivisions_count
                FROM buildings b
                JOIN military_units mu ON mu.unit_id = b.unit_id
                LEFT JOIN subdivision_buildings sb ON sb.building_id = b.building_id
                WHERE b.assignable = TRUE
                GROUP BY b.building_id, b.name, mu.name
                HAVING COUNT(sb.subdivision_id) > :threshold
                ORDER BY subdivisions_count DESC, b.name
                """, Map.of("threshold", BUILDING_OVERLOAD_THRESHOLD), (rs, rowNum) -> alert(
                "BUILDING_OVERLOADED",
                "MEDIUM",
                "Перегрузка сооружения",
                rs.getString("building_name") + " размещает подразделений: " + rs.getLong("subdivisions_count") + ".",
                ObjectType.BUILDING,
                rs.getLong("building_id"),
                Map.of(
                        "buildingName", rs.getString("building_name"),
                        "unitName", rs.getString("unit_name"),
                        "subdivisionsCount", rs.getLong("subdivisions_count")
                ),
                List.of(
                        new AlertActionDto("Открыть сооружения", "/buildings", null),
                        new AlertActionDto("Открыть терминал", "/intelligence", "FIND_BUILDING_USAGE")
                )
        )).stream().filter(alert -> canRead(user, alert)).toList();
    }

    private List<TacticalAlertDto> specialtiesWithoutSpecialists(UserContext user) {
        return jdbcTemplate.query("""
                SELECT s.specialty_id, s.name AS specialty_name
                FROM specialties s
                WHERE NOT EXISTS (
                    SELECT 1 FROM personnel_specialties ps WHERE ps.specialty_id = s.specialty_id
                )
                ORDER BY s.name
                """, Map.of(), (rs, rowNum) -> alert(
                "SPECIALTY_WITHOUT_SPECIALISTS",
                "MEDIUM",
                "Недостаток специалистов",
                "По специальности \"" + rs.getString("specialty_name") + "\" нет назначенных военнослужащих.",
                ObjectType.SPECIALTY,
                rs.getLong("specialty_id"),
                Map.of("specialtyName", rs.getString("specialty_name")),
                List.of(new AlertActionDto("Открыть терминал", "/intelligence", "FIND_SPECIALTY_COVERAGE"))
        )).stream()
                .filter(alert -> permissionService.canRead(user, ObjectType.SPECIALTY, alert.objectId()))
                .toList();
    }

    private List<TacticalAlertDto> equipmentQuantityExceeded(UserContext user) {
        return jdbcTemplate.query("""
                SELECT mu.unit_id, mu.name AS unit_name, et.name AS equipment_type, eiu.quantity
                FROM equipment_in_units eiu
                JOIN military_units mu ON mu.unit_id = eiu.unit_id
                JOIN equipment_types et ON et.type_id = eiu.type_id
                WHERE eiu.quantity > :threshold
                ORDER BY eiu.quantity DESC
                """, Map.of("threshold", EQUIPMENT_EXCEEDED_THRESHOLD), (rs, rowNum) -> alert(
                "EQUIPMENT_QUANTITY_EXCEEDED",
                "CRITICAL",
                "Превышение количества техники",
                rs.getString("unit_name") + ": зафиксировано повышенное количество техники \"" + rs.getString("equipment_type") + "\".",
                ObjectType.MILITARY_UNIT,
                rs.getLong("unit_id"),
                Map.of("unitName", rs.getString("unit_name"), "equipmentType", rs.getString("equipment_type"), "quantity", rs.getInt("quantity")),
                List.of(
                        new AlertActionDto("Показать технику", "/equipment?unitId=" + rs.getLong("unit_id"), "FIND_UNIT_EQUIPMENT"),
                        new AlertActionDto("Открыть терминал", "/intelligence", "FIND_EQUIPMENT_AVAILABILITY")
                )
        )).stream().filter(alert -> canRead(user, alert)).toList();
    }

    private List<TacticalAlertDto> weaponQuantityExceeded(UserContext user) {
        return jdbcTemplate.query("""
                SELECT mu.unit_id, mu.name AS unit_name, wt.name AS weapon_type, wiu.quantity
                FROM weapon_in_units wiu
                JOIN military_units mu ON mu.unit_id = wiu.unit_id
                JOIN weapon_types wt ON wt.type_id = wiu.type_id
                WHERE wiu.quantity > :threshold
                ORDER BY wiu.quantity DESC
                """, Map.of("threshold", WEAPON_EXCEEDED_THRESHOLD), (rs, rowNum) -> alert(
                "WEAPON_QUANTITY_EXCEEDED",
                "CRITICAL",
                "Превышение количества вооружения",
                rs.getString("unit_name") + ": зафиксировано повышенное количество вооружения \"" + rs.getString("weapon_type") + "\".",
                ObjectType.MILITARY_UNIT,
                rs.getLong("unit_id"),
                Map.of("unitName", rs.getString("unit_name"), "weaponType", rs.getString("weapon_type"), "quantity", rs.getInt("quantity")),
                List.of(
                        new AlertActionDto("Показать вооружение", "/weapons?unitId=" + rs.getLong("unit_id"), "FIND_UNIT_WEAPONS"),
                        new AlertActionDto("Открыть терминал", "/intelligence", "FIND_WEAPON_AVAILABILITY")
                )
        )).stream().filter(alert -> canRead(user, alert)).toList();
    }

    private TacticalAlertDto alert(
            String type,
            String severity,
            String title,
            String message,
            ObjectType objectType,
            Long objectId,
            Map<String, Object> details,
            List<AlertActionDto> actions
    ) {
        return new TacticalAlertDto(
                type + ":" + objectType.name() + ":" + objectId,
                type,
                severity,
                title,
                message,
                objectType.name(),
                objectId,
                new LinkedHashMap<>(details),
                actions
        );
    }

    private boolean canRead(UserContext user, TacticalAlertDto alert) {
        return permissionService.canRead(user, ObjectType.from(alert.objectType()), alert.objectId());
    }
}
