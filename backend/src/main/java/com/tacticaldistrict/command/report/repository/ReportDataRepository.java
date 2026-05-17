package com.tacticaldistrict.command.report.repository;

import com.tacticaldistrict.command.hierarchy.dto.ObjectPassportResponse;
import com.tacticaldistrict.command.hierarchy.repository.HierarchyQueryRepository;
import com.tacticaldistrict.command.report.dto.BuildingUsageDto;
import com.tacticaldistrict.command.report.dto.ReportBuildingSummaryDto;
import com.tacticaldistrict.command.report.dto.ReportCommanderDto;
import com.tacticaldistrict.command.report.dto.ReportEquipmentSummaryDto;
import com.tacticaldistrict.command.report.dto.ReportObjectDto;
import com.tacticaldistrict.command.report.dto.ReportPersonnelSummaryDto;
import com.tacticaldistrict.command.report.dto.ReportSpecialtySummaryDto;
import com.tacticaldistrict.command.report.dto.ReportWeaponSummaryDto;
import com.tacticaldistrict.command.report.dto.ResourceQuantityDto;
import com.tacticaldistrict.command.report.model.ReportObjectType;
import com.tacticaldistrict.command.security.model.ObjectType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReportDataRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final HierarchyQueryRepository hierarchyQueryRepository;

    public ReportObjectDto objectInfo(ReportObjectType type, Long id) {
        ObjectPassportResponse passport = hierarchyQueryRepository.passport(type.toObjectType(), id);
        List<String> path = passport.breadcrumbs().stream().map(node -> node.label()).toList();
        String parentName = path.size() > 1 ? path.get(path.size() - 2) : null;
        Object location = passport.details().get("locationName");
        return new ReportObjectDto(
                type,
                id,
                passport.name(),
                parentName,
                passport.status(),
                location == null ? null : location.toString(),
                path
        );
    }

    public List<ReportCommanderDto> commanders(ReportObjectType type, Long id) {
        return hierarchyQueryRepository.breadcrumbPath(type.toObjectType(), id)
                .stream()
                .filter(node -> node.commander() != null)
                .map(node -> new ReportCommanderDto(
                        node.commander().personnelId(),
                        node.commander().fullName(),
                        node.commander().rankName(),
                        commanderPosition(node.type()),
                        node.label()
                ))
                .toList();
    }

    public List<Long> unitIds(ReportObjectType type, Long id) {
        return switch (type) {
            case MILITARY_UNIT -> List.of(id);
            case COMPANY, PLATOON, SQUAD -> jdbcTemplate.query("""
                    SELECT DISTINCT unit_id
                    FROM subdivisions
                    WHERE subdivision_id = :id
                    """, Map.of("id", id), (rs, rowNum) -> rs.getLong("unit_id"));
            case DISTRICT, ARMY, FORMATION, BRIGADE -> jdbcTemplate.query("""
                    SELECT DISTINCT mu.unit_id
                    FROM military_units mu
                    JOIN v_formation_closure fc ON fc.descendant_formation_id = mu.formation_id
                    WHERE fc.root_formation_id = :id
                    ORDER BY mu.unit_id
                    """, Map.of("id", id), (rs, rowNum) -> rs.getLong("unit_id"));
        };
    }

    public ReportPersonnelSummaryDto personnelSummary(ReportObjectType type, Long id) {
        String scope = personnelScope(type);
        Map<String, Object> params = Map.of("id", id);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT p.personnel_id,
                       mr.name AS rank_name,
                       mr.category AS rank_category,
                       s.name AS subdivision_name
                FROM personnel p
                JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
                JOIN military_units mu ON mu.unit_id = s.unit_id
                LEFT JOIN personnel_ranks pr ON pr.personnel_id = p.personnel_id
                LEFT JOIN military_ranks mr ON mr.rank_id = pr.rank_id
                WHERE 1 = 1
                """ + scope, params);

        Map<String, Long> byRank = countBy(rows, "rank_name", "Без звания");
        Map<String, Long> bySubdivision = countBy(rows, "subdivision_name", "Без подразделения");
        long officers = rows.stream().filter(row -> "Офицерский".equals(row.get("rank_category"))).count();
        long enlisted = rows.stream().filter(row -> "Сержантский и Рядовой".equals(row.get("rank_category"))).count();
        return new ReportPersonnelSummaryDto(rows.size(), officers, enlisted, commanders(type, id).size(), byRank, bySubdivision);
    }

    public ReportEquipmentSummaryDto equipmentSummary(List<Long> unitIds) {
        if (unitIds.isEmpty()) {
            return new ReportEquipmentSummaryDto(0, 0, 0, List.of(), List.of());
        }
        Map<String, Object> params = Map.of("unitIds", unitIds);
        List<ResourceQuantityDto> top = jdbcTemplate.query("""
                SELECT et.name AS type_name, ec.name AS category_name, SUM(eiu.quantity) AS quantity
                FROM equipment_in_units eiu
                JOIN equipment_types et ON et.type_id = eiu.type_id
                JOIN equipment_categories ec ON ec.category_id = et.category_id
                WHERE eiu.unit_id IN (:unitIds)
                GROUP BY et.type_id, et.name, ec.name
                ORDER BY quantity DESC, et.name
                LIMIT 8
                """, params, (rs, rowNum) -> new ResourceQuantityDto(
                rs.getString("type_name"),
                rs.getString("category_name"),
                rs.getLong("quantity")
        ));
        Long totalQuantity = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0)
                FROM equipment_in_units
                WHERE unit_id IN (:unitIds)
                """, params, Long.class);
        Long types = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT type_id)
                FROM equipment_in_units
                WHERE unit_id IN (:unitIds)
                """, params, Long.class);
        List<String> missing = jdbcTemplate.query("""
                SELECT mu.name
                FROM military_units mu
                WHERE mu.unit_id IN (:unitIds)
                  AND NOT EXISTS (SELECT 1 FROM equipment_in_units eiu WHERE eiu.unit_id = mu.unit_id)
                ORDER BY mu.name
                """, params, (rs, rowNum) -> rs.getString("name"));
        return new ReportEquipmentSummaryDto(valueOrZero(totalQuantity), valueOrZero(types), missing.size(), top, missing);
    }

    public ReportWeaponSummaryDto weaponSummary(List<Long> unitIds) {
        if (unitIds.isEmpty()) {
            return new ReportWeaponSummaryDto(0, 0, 0, List.of(), List.of());
        }
        Map<String, Object> params = Map.of("unitIds", unitIds);
        List<ResourceQuantityDto> top = jdbcTemplate.query("""
                SELECT wt.name AS type_name, wc.name AS category_name, SUM(wiu.quantity) AS quantity
                FROM weapon_in_units wiu
                JOIN weapon_types wt ON wt.type_id = wiu.type_id
                JOIN weapon_categories wc ON wc.category_id = wt.category_id
                WHERE wiu.unit_id IN (:unitIds)
                GROUP BY wt.type_id, wt.name, wc.name
                ORDER BY quantity DESC, wt.name
                LIMIT 8
                """, params, (rs, rowNum) -> new ResourceQuantityDto(
                rs.getString("type_name"),
                rs.getString("category_name"),
                rs.getLong("quantity")
        ));
        Long totalQuantity = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0)
                FROM weapon_in_units
                WHERE unit_id IN (:unitIds)
                """, params, Long.class);
        Long types = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT type_id)
                FROM weapon_in_units
                WHERE unit_id IN (:unitIds)
                """, params, Long.class);
        List<String> missing = jdbcTemplate.query("""
                SELECT mu.name
                FROM military_units mu
                WHERE mu.unit_id IN (:unitIds)
                  AND NOT EXISTS (SELECT 1 FROM weapon_in_units wiu WHERE wiu.unit_id = mu.unit_id)
                ORDER BY mu.name
                """, params, (rs, rowNum) -> rs.getString("name"));
        return new ReportWeaponSummaryDto(valueOrZero(totalQuantity), valueOrZero(types), missing.size(), top, missing);
    }

    public ReportBuildingSummaryDto buildingSummary(List<Long> unitIds) {
        if (unitIds.isEmpty()) {
            return new ReportBuildingSummaryDto(0, 0, 0, List.of());
        }
        Map<String, Object> params = Map.of("unitIds", unitIds);
        List<BuildingUsageDto> buildings = jdbcTemplate.query("""
                SELECT b.building_id, b.name AS building_name, mu.name AS unit_name, COUNT(sb.subdivision_id) AS subdivisions_count
                FROM buildings b
                JOIN military_units mu ON mu.unit_id = b.unit_id
                LEFT JOIN subdivision_buildings sb ON sb.building_id = b.building_id
                WHERE b.unit_id IN (:unitIds)
                GROUP BY b.building_id, b.name, mu.name
                ORDER BY mu.name, b.name
                """, params, (rs, rowNum) -> new BuildingUsageDto(
                rs.getLong("building_id"),
                rs.getString("building_name"),
                rs.getString("unit_name"),
                rs.getLong("subdivisions_count")
        ));
        List<BuildingUsageDto> problem = buildings.stream()
                .filter(building -> building.subdivisionsCount() == 0 || building.subdivisionsCount() > 3)
                .toList();
        long unused = buildings.stream().filter(building -> building.subdivisionsCount() == 0).count();
        long overloaded = buildings.stream().filter(building -> building.subdivisionsCount() > 3).count();
        return new ReportBuildingSummaryDto(buildings.size(), unused, overloaded, problem);
    }

    public ReportSpecialtySummaryDto specialtySummary(ReportObjectType type, Long id) {
        String scope = personnelScope(type);
        Map<String, Object> params = Map.of("id", id);
        Map<String, Long> top = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT sp.name AS specialty_name, COUNT(DISTINCT p.personnel_id) AS specialists_count
                FROM specialties sp
                JOIN personnel_specialties ps ON ps.specialty_id = sp.specialty_id
                JOIN personnel p ON p.personnel_id = ps.personnel_id
                JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
                JOIN military_units mu ON mu.unit_id = s.unit_id
                WHERE 1 = 1
                """ + scope + """
                GROUP BY sp.specialty_id, sp.name
                ORDER BY specialists_count DESC, sp.name
                LIMIT 8
                """, params, rs -> {
            top.put(rs.getString("specialty_name"), rs.getLong("specialists_count"));
        });
        List<String> missing = jdbcTemplate.query("""
                SELECT sp.name
                FROM specialties sp
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM personnel_specialties ps
                    JOIN personnel p ON p.personnel_id = ps.personnel_id
                    JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
                    JOIN military_units mu ON mu.unit_id = s.unit_id
                    WHERE ps.specialty_id = sp.specialty_id
                    """ + scope + """
                )
                ORDER BY sp.name
                """, params, (rs, rowNum) -> rs.getString("name"));
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM specialties", Map.of(), Long.class);
        return new ReportSpecialtySummaryDto(valueOrZero(total), top.size(), missing.size(), missing, top);
    }

    public Long buildingUnitId(Long buildingId) {
        return jdbcTemplate.query("""
                SELECT unit_id FROM buildings WHERE building_id = :buildingId
                """, Map.of("buildingId", buildingId), rs -> rs.next() ? rs.getLong("unit_id") : null);
    }

    private String personnelScope(ReportObjectType type) {
        return switch (type) {
            case MILITARY_UNIT -> " AND mu.unit_id = :id ";
            case COMPANY, PLATOON, SQUAD -> """
                    AND s.subdivision_id IN (
                        WITH RECURSIVE sub_tree AS (
                            SELECT subdivision_id FROM subdivisions WHERE subdivision_id = :id
                            UNION ALL
                            SELECT child.subdivision_id
                            FROM subdivisions child
                            JOIN sub_tree parent ON child.parent_id = parent.subdivision_id
                        )
                        SELECT subdivision_id FROM sub_tree
                    )
                    """;
            case DISTRICT, ARMY, FORMATION, BRIGADE -> """
                    AND EXISTS (
                        SELECT 1
                        FROM v_formation_closure fc
                        WHERE fc.root_formation_id = :id
                          AND fc.descendant_formation_id = mu.formation_id
                    )
                    """;
        };
    }

    private Map<String, Long> countBy(List<Map<String, Object>> rows, String key, String fallback) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String value = row.get(key) == null ? fallback : row.get(key).toString();
            result.put(value, result.getOrDefault(value, 0L) + 1);
        }
        return result;
    }

    private long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }

    private String commanderPosition(String objectType) {
        return switch (objectType) {
            case "MILITARY_UNIT" -> "Командир военной части";
            case "COMPANY" -> "Командир роты";
            case "PLATOON" -> "Командир взвода";
            case "SQUAD" -> "Командир отделения";
            default -> "Командир соединения";
        };
    }
}
