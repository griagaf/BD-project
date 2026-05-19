package com.tacticaldistrict.command.intelligence.service;

import com.tacticaldistrict.command.intelligence.dto.ExecuteQueryRequest;
import com.tacticaldistrict.command.intelligence.dto.QueryParameterMetadataDto;
import com.tacticaldistrict.command.intelligence.dto.QueryResultDto;
import com.tacticaldistrict.command.intelligence.dto.QueryScopeDto;
import com.tacticaldistrict.command.intelligence.model.QueryTemplate;
import com.tacticaldistrict.command.security.access.PermissionService;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QueryExecutorService {

    private static final List<String> FORMATION_SCOPE_TYPES = List.of("DISTRICT", "FORMATION", "ARMY", "CORPS", "DIVISION", "BRIGADE");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final UserContextProvider userContextProvider;
    private final PermissionService permissionService;

    @Transactional(readOnly = true)
    public QueryResultDto execute(QueryTemplate template, ExecuteQueryRequest request) {
        UserContext user = userContextProvider.current();
        checkRolePermissions(user, template);
        validate(template, request);
        checkScope(user, request.scope());

        QueryDefinition query = query(template, request);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query.sql(), query.parameters())
                .stream()
                .map(this::normalize)
                .filter(row -> rowInScope(user, row))
                .toList();
        List<String> columns = rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet());
        return new QueryResultDto(
                template.name(),
                preview(template, request),
                columns,
                rows,
                rows.size(),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportCsv(QueryTemplate template, ExecuteQueryRequest request) {
        QueryResultDto result = execute(template, request);
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append(String.join(",", result.columns())).append("\n");
        for (Map<String, Object> row : result.rows()) {
            csv.append(result.columns().stream()
                    .map(column -> csvCell(row.get(column)))
                    .collect(java.util.stream.Collectors.joining(",")))
                    .append("\n");
        }
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(template.name().toLowerCase() + ".csv")
                        .build()
                        .toString())
                .body(bytes);
    }

    private void checkRolePermissions(UserContext user, QueryTemplate template) {
        boolean allowed = template.requiredPermissions().stream().allMatch(user::hasPermission);
        if (!allowed) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private void validate(QueryTemplate template, ExecuteQueryRequest request) {
        Map<String, Object> parameters = parameters(request);
        for (QueryParameterMetadataDto parameter : template.parameters()) {
            if (Boolean.TRUE.equals(parameter.required()) && isBlank(parameters.get(parameter.name()))) {
                throw new IllegalArgumentException("Required query parameter is missing: " + parameter.name());
            }
        }
    }

    private void checkScope(UserContext user, QueryScopeDto scope) {
        if (scope == null || scope.type() == null || scope.id() == null) {
            return;
        }
        ObjectType objectType = objectType(scope.type());
        permissionService.checkRead(user, objectType, scope.id());
    }

    private QueryDefinition query(QueryTemplate template, ExecuteQueryRequest request) {
        Map<String, Object> params = new LinkedHashMap<>(parameters(request));
        QueryScopeDto scope = request == null ? null : request.scope();
        applyScopeParams(scope, params);

        return switch (template) {
            case FIND_UNITS_IN_FORMATION -> unitsInFormation(params);
            case FIND_OFFICERS -> personnelByRankCategory(params, "Офицерский");
            case FIND_ENLISTED_PERSONNEL -> personnelByRankCategory(params, "Сержантский и Рядовой");
            case FIND_PERSONNEL_COMMAND_CHAIN -> personnelCommandChain(params);
            case FIND_UNIT_LOCATIONS -> unitLocations(params);
            case FIND_UNIT_EQUIPMENT -> unitEquipment(params);
            case FIND_BUILDING_USAGE -> buildingUsage(params);
            case FIND_EQUIPMENT_AVAILABILITY -> equipmentAvailability(params);
            case FIND_UNIT_WEAPONS -> unitWeapons(params);
            case FIND_SPECIALTY_COVERAGE -> specialtyCoverage(params);
            case FIND_SPECIALISTS -> specialists(params);
            case FIND_WEAPON_AVAILABILITY -> weaponAvailability(params);
            case FIND_FORMATION_UNIT_EXTREMES -> formationUnitExtremes(params);
        };
    }

    private QueryDefinition unitsInFormation(Map<String, Object> params) {
        String scope = formationWhere("mf.formation_id", params);
        return new QueryDefinition("""
                SELECT mu.unit_id,
                       mu.name AS unit_name,
                       mf.formation_id,
                       mf.name AS parent_formation_name,
                       mf.formation_type AS parent_formation_type,
                       trim(concat(cp.last_name, ' ', cp.first_name, ' ', coalesce(cp.middle_name, ''))) AS commander_name,
                       mr.name AS commander_rank
                FROM military_units mu
                JOIN military_formations mf ON mf.formation_id = mu.formation_id
                LEFT JOIN personnel cp ON cp.personnel_id = mu.commander_id
                LEFT JOIN personnel_ranks pr ON pr.personnel_id = cp.personnel_id
                LEFT JOIN military_ranks mr ON mr.rank_id = pr.rank_id
                WHERE 1 = 1
                """ + scope + """
                ORDER BY mf.formation_type, mf.name, mu.name
                """, params);
    }

    private QueryDefinition personnelByRankCategory(Map<String, Object> params, String category) {
        params.put("rankCategory", category);
        String rankFilter = isBlank(params.get("rankName")) ? "" : " AND mr.name = :rankName ";
        String scope = personnelScopeWhere(params);
        return new QueryDefinition("""
                SELECT p.personnel_id,
                       p.last_name,
                       p.first_name,
                       p.middle_name,
                       p.personal_number,
                       mr.name AS rank_name,
                       mu.unit_id,
                       mu.name AS unit_name,
                       s.name AS subdivision_name
                FROM personnel p
                JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
                JOIN military_units mu ON mu.unit_id = s.unit_id
                JOIN personnel_ranks pr ON pr.personnel_id = p.personnel_id
                JOIN military_ranks mr ON mr.rank_id = pr.rank_id
                WHERE mr.category = :rankCategory
                """ + rankFilter + scope + """
                ORDER BY mr.name, p.last_name, p.first_name, p.middle_name
                """, params);
    }

    private QueryDefinition personnelCommandChain(Map<String, Object> params) {
        Long personnelId = longValue(params.get("personnelId"));
        params.put("personnelId", personnelId);
        return new QueryDefinition("""
                WITH RECURSIVE subdivision_chain AS (
                    SELECT s.subdivision_id, s.name, s.type, s.parent_id, s.commander_id, 1 AS level_no
                    FROM personnel p
                    JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
                    WHERE p.personnel_id = :personnelId
                    UNION ALL
                    SELECT parent_s.subdivision_id, parent_s.name, parent_s.type, parent_s.parent_id, parent_s.commander_id, sc.level_no + 1
                    FROM subdivisions parent_s
                    JOIN subdivision_chain sc ON sc.parent_id = parent_s.subdivision_id
                ),
                person_unit AS (
                    SELECT mu.unit_id, mu.name AS unit_name, mu.formation_id, mu.commander_id
                    FROM personnel p
                    JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
                    JOIN military_units mu ON mu.unit_id = s.unit_id
                    WHERE p.personnel_id = :personnelId
                ),
                formation_chain AS (
                    SELECT mf.formation_id, mf.name, mf.formation_type, mf.parent_id, mf.commander_id, 1 AS level_no
                    FROM person_unit pu
                    JOIN military_formations mf ON mf.formation_id = pu.formation_id
                    UNION ALL
                    SELECT parent_f.formation_id, parent_f.name, parent_f.formation_type, parent_f.parent_id, parent_f.commander_id, fc.level_no + 1
                    FROM military_formations parent_f
                    JOIN formation_chain fc ON fc.parent_id = parent_f.formation_id
                )
                SELECT 0 AS sort_group, 0 AS level_no, p.personnel_id, NULL::BIGINT AS unit_id, NULL::BIGINT AS formation_id,
                       'PERSONNEL' AS object_type,
                       trim(concat(p.last_name, ' ', p.first_name, ' ', coalesce(p.middle_name, ''))) AS object_name,
                       mr.name AS extra_info
                FROM personnel p
                LEFT JOIN personnel_ranks pr ON pr.personnel_id = p.personnel_id
                LEFT JOIN military_ranks mr ON mr.rank_id = pr.rank_id
                WHERE p.personnel_id = :personnelId
                UNION ALL
                SELECT 1, sc.level_no, NULL, NULL, NULL, sc.type, sc.name,
                       trim(concat(cp.last_name, ' ', cp.first_name, ' ', coalesce(cp.middle_name, '')))
                FROM subdivision_chain sc
                LEFT JOIN personnel cp ON cp.personnel_id = sc.commander_id
                UNION ALL
                SELECT 2, 1, NULL, pu.unit_id, NULL, 'MILITARY_UNIT', pu.unit_name,
                       trim(concat(cp.last_name, ' ', cp.first_name, ' ', coalesce(cp.middle_name, '')))
                FROM person_unit pu
                LEFT JOIN personnel cp ON cp.personnel_id = pu.commander_id
                UNION ALL
                SELECT 3, fc.level_no, NULL, NULL, fc.formation_id, fc.formation_type, fc.name,
                       trim(concat(cp.last_name, ' ', cp.first_name, ' ', coalesce(cp.middle_name, '')))
                FROM formation_chain fc
                LEFT JOIN personnel cp ON cp.personnel_id = fc.commander_id
                ORDER BY sort_group, level_no
                """, params);
    }

    private QueryDefinition unitLocations(Map<String, Object> params) {
        String scope = unitScopeWhere(params);
        return new QueryDefinition("""
                SELECT mu.unit_id,
                       mu.name AS unit_name,
                       mf.formation_id,
                       mf.name AS formation_name,
                       l.city,
                       l.address
                FROM military_units mu
                JOIN military_formations mf ON mf.formation_id = mu.formation_id
                LEFT JOIN locations l ON l.location_id = mu.location_id
                WHERE 1 = 1
                """ + scope + """
                ORDER BY l.city, l.address, mu.name
                """, params);
    }

    private QueryDefinition unitEquipment(Map<String, Object> params) {
        String filters = unitScopeWhere(params)
                + (isBlank(params.get("equipmentCategory")) ? "" : " AND ec.name = :equipmentCategory ")
                + (isBlank(params.get("equipmentType")) ? "" : " AND et.name = :equipmentType ");
        return new QueryDefinition("""
                SELECT mu.unit_id,
                       mu.name AS unit_name,
                       mf.formation_id,
                       ec.name AS equipment_category,
                       et.name AS equipment_type,
                       eiu.quantity
                FROM equipment_in_units eiu
                JOIN military_units mu ON mu.unit_id = eiu.unit_id
                JOIN military_formations mf ON mf.formation_id = mu.formation_id
                JOIN equipment_types et ON et.type_id = eiu.type_id
                JOIN equipment_categories ec ON ec.category_id = et.category_id
                WHERE 1 = 1
                """ + filters + """
                ORDER BY mu.name, ec.name, et.name
                """, params);
    }

    private QueryDefinition buildingUsage(Map<String, Object> params) {
        String usage = stringValue(params.getOrDefault("usage", "ALL"));
        String filters = unitScopeWhere(params);
        if ("EMPTY".equals(usage)) {
            filters += " AND COUNT(sb.subdivision_id) = 0 ";
        } else if ("OVERLOADED".equals(usage)) {
            filters += " AND COUNT(sb.subdivision_id) > 3 ";
        }
        String having = filters.contains("COUNT(")
                ? " HAVING " + filters.substring(filters.indexOf("AND COUNT(") + 4)
                : "";
        String where = filters.contains("AND COUNT(")
                ? filters.substring(0, filters.indexOf("AND COUNT("))
                : filters;
        return new QueryDefinition("""
                SELECT b.building_id,
                       b.name AS building_name,
                       mu.unit_id,
                       mu.name AS unit_name,
                       COUNT(sb.subdivision_id) AS subdivisions_count
                FROM buildings b
                JOIN military_units mu ON mu.unit_id = b.unit_id
                LEFT JOIN subdivision_buildings sb ON sb.building_id = b.building_id
                WHERE 1 = 1
                """ + where + """
                GROUP BY b.building_id, b.name, mu.unit_id, mu.name
                """ + having + """
                ORDER BY mu.name, b.name
                """, params);
    }

    private QueryDefinition equipmentAvailability(Map<String, Object> params) {
        params.putIfAbsent("minQuantity", 0);
        String condition = stringValue(params.getOrDefault("condition", "WITH_QUANTITY_GT"));
        if ("WITHOUT".equals(condition)) {
            return new QueryDefinition("""
                    SELECT mu.unit_id,
                           mu.name AS unit_name
                    FROM military_units mu
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM equipment_in_units eiu
                        JOIN equipment_types et ON et.type_id = eiu.type_id
                        WHERE eiu.unit_id = mu.unit_id
                          AND et.name = :equipmentType
                    )
                    """ + unitScopeWhere(params).replace("AND mu.", "AND mu.") + """
                    ORDER BY mu.name
                    """, params);
        }
        return new QueryDefinition("""
                SELECT mu.unit_id,
                       mu.name AS unit_name,
                       et.name AS equipment_type,
                       eiu.quantity
                FROM equipment_in_units eiu
                JOIN military_units mu ON mu.unit_id = eiu.unit_id
                JOIN equipment_types et ON et.type_id = eiu.type_id
                WHERE et.name = :equipmentType
                  AND eiu.quantity > :minQuantity
                """ + unitScopeWhere(params) + """
                ORDER BY mu.name
                """, params);
    }

    private QueryDefinition unitWeapons(Map<String, Object> params) {
        String filters = unitScopeWhere(params)
                + (isBlank(params.get("weaponCategory")) ? "" : " AND wc.name = :weaponCategory ")
                + (isBlank(params.get("weaponType")) ? "" : " AND wt.name = :weaponType ");
        return new QueryDefinition("""
                SELECT mu.unit_id,
                       mu.name AS unit_name,
                       mf.formation_id,
                       wc.name AS weapon_category,
                       wt.name AS weapon_type,
                       wiu.quantity
                FROM weapon_in_units wiu
                JOIN military_units mu ON mu.unit_id = wiu.unit_id
                JOIN military_formations mf ON mf.formation_id = mu.formation_id
                JOIN weapon_types wt ON wt.type_id = wiu.type_id
                JOIN weapon_categories wc ON wc.category_id = wt.category_id
                WHERE 1 = 1
                """ + filters + """
                ORDER BY mu.name, wc.name, wt.name
                """, params);
    }

    private QueryDefinition specialtyCoverage(Map<String, Object> params) {
        params.putIfAbsent("minCount", 1);
        String condition = stringValue(params.getOrDefault("condition", "WITH_SPECIALISTS"));
        String scope = personnelScopeWhere(params);
        if ("WITHOUT_SPECIALISTS".equals(condition)) {
            return new QueryDefinition("""
                    WITH scoped_specialties AS (
                        SELECT DISTINCT ps.specialty_id, p.personnel_id
                        FROM personnel p
                        JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
                        JOIN military_units mu ON mu.unit_id = s.unit_id
                        JOIN personnel_specialties ps ON ps.personnel_id = p.personnel_id
                        WHERE 1 = 1
                        """ + scope + """
                    )
                    SELECT s.specialty_id,
                           s.name AS specialty_name,
                           COUNT(ss.personnel_id) AS specialists_count
                    FROM specialties s
                    LEFT JOIN scoped_specialties ss ON ss.specialty_id = s.specialty_id
                    GROUP BY s.specialty_id, s.name
                    HAVING COUNT(ss.personnel_id) = 0
                    ORDER BY s.name
                    """, params);
        }
        return new QueryDefinition("""
                SELECT sp.specialty_id,
                       sp.name AS specialty_name,
                       COUNT(DISTINCT p.personnel_id) AS specialists_count
                FROM personnel p
                JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
                JOIN military_units mu ON mu.unit_id = s.unit_id
                JOIN personnel_specialties ps ON ps.personnel_id = p.personnel_id
                JOIN specialties sp ON sp.specialty_id = ps.specialty_id
                WHERE 1 = 1
                """ + scope + """
                GROUP BY sp.specialty_id, sp.name
                HAVING COUNT(DISTINCT p.personnel_id) >= :minCount
                ORDER BY sp.name
                """, params);
    }

    private QueryDefinition specialists(Map<String, Object> params) {
        String subdivisionFilter = isBlank(params.get("subdivisionName")) ? "" : " AND s.name = :subdivisionName ";
        return new QueryDefinition("""
                SELECT p.personnel_id,
                       p.last_name,
                       p.first_name,
                       p.middle_name,
                       mr.name AS rank_name,
                       sp.name AS specialty_name,
                       mu.unit_id,
                       mu.name AS unit_name,
                       s.name AS subdivision_name
                FROM personnel p
                JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
                JOIN military_units mu ON mu.unit_id = s.unit_id
                LEFT JOIN personnel_ranks pr ON pr.personnel_id = p.personnel_id
                LEFT JOIN military_ranks mr ON mr.rank_id = pr.rank_id
                JOIN personnel_specialties ps ON ps.personnel_id = p.personnel_id
                JOIN specialties sp ON sp.specialty_id = ps.specialty_id
                WHERE sp.name = :specialtyName
                """ + subdivisionFilter + personnelScopeWhere(params) + """
                ORDER BY p.last_name, p.first_name, p.middle_name
                """, params);
    }

    private QueryDefinition weaponAvailability(Map<String, Object> params) {
        params.putIfAbsent("minQuantity", 0);
        String condition = stringValue(params.getOrDefault("condition", "WITH_QUANTITY_GT"));
        if ("WITHOUT".equals(condition)) {
            return new QueryDefinition("""
                    SELECT mu.unit_id,
                           mu.name AS unit_name
                    FROM military_units mu
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM weapon_in_units wiu
                        JOIN weapon_types wt ON wt.type_id = wiu.type_id
                        WHERE wiu.unit_id = mu.unit_id
                          AND wt.name = :weaponType
                    )
                    """ + unitScopeWhere(params) + """
                    ORDER BY mu.name
                    """, params);
        }
        return new QueryDefinition("""
                SELECT mu.unit_id,
                       mu.name AS unit_name,
                       wt.name AS weapon_type,
                       wiu.quantity
                FROM weapon_in_units wiu
                JOIN military_units mu ON mu.unit_id = wiu.unit_id
                JOIN weapon_types wt ON wt.type_id = wiu.type_id
                WHERE wt.name = :weaponType
                  AND wiu.quantity > :minQuantity
                """ + unitScopeWhere(params) + """
                ORDER BY mu.name
                """, params);
    }

    private QueryDefinition formationUnitExtremes(Map<String, Object> params) {
        String direction = stringValue(params.getOrDefault("direction", "MAX"));
        String order = "MIN".equals(direction) ? "ASC" : "DESC";
        return new QueryDefinition("""
                WITH units_count AS (
                    SELECT fc.root_formation_id AS formation_id,
                           fc.root_formation_name AS formation_name,
                           fc.root_formation_type AS formation_type,
                           COUNT(mu.unit_id) AS units_count
                    FROM v_formation_closure fc
                    LEFT JOIN military_units mu ON mu.formation_id = fc.descendant_formation_id
                    WHERE fc.root_formation_type IN ('Армия', 'Дивизия', 'Корпус', 'Бригада')
                    GROUP BY fc.root_formation_id, fc.root_formation_name, fc.root_formation_type
                ),
                ranked AS (
                    SELECT uc.*, RANK() OVER (ORDER BY uc.units_count """ + " " + order + " " + """
                    ) AS rank_no
                    FROM units_count uc
                )
                SELECT formation_id, formation_type, formation_name, units_count
                FROM ranked
                WHERE rank_no = 1
                ORDER BY formation_type, formation_name
                """, params);
    }

    private String personnelScopeWhere(Map<String, Object> params) {
        if (params.containsKey("unitId")) {
            return " AND mu.unit_id = :unitId ";
        }
        if (params.containsKey("formationId")) {
            return """
                    AND EXISTS (
                        SELECT 1
                        FROM v_formation_closure fc
                        WHERE fc.root_formation_id = :formationId
                          AND fc.descendant_formation_id = mu.formation_id
                    )
                    """;
        }
        return "";
    }

    private String unitScopeWhere(Map<String, Object> params) {
        if (params.containsKey("unitId")) {
            return " AND mu.unit_id = :unitId ";
        }
        return formationWhere("mu.formation_id", params);
    }

    private String formationWhere(String formationColumn, Map<String, Object> params) {
        if (!params.containsKey("formationId")) {
            return "";
        }
        return """
                AND EXISTS (
                    SELECT 1
                    FROM v_formation_closure fc
                    WHERE fc.root_formation_id = :formationId
                      AND fc.descendant_formation_id = """ + formationColumn + """
                )
                """;
    }

    private void applyScopeParams(QueryScopeDto scope, Map<String, Object> params) {
        if (scope == null || scope.id() == null || scope.type() == null) {
            return;
        }
        String type = scope.type().trim().toUpperCase(java.util.Locale.ROOT);
        if ("UNIT".equals(type) || "MILITARY_UNIT".equals(type)) {
            params.putIfAbsent("unitId", scope.id());
        } else if (FORMATION_SCOPE_TYPES.contains(type)) {
            params.putIfAbsent("formationId", scope.id());
        } else if ("PERSONNEL".equals(type)) {
            params.putIfAbsent("personnelId", scope.id());
        }
    }

    private boolean rowInScope(UserContext user, Map<String, Object> row) {
        if (row.containsKey("personnel_id") && row.get("personnel_id") != null) {
            return permissionService.canRead(user, ObjectType.PERSONNEL, longValue(row.get("personnel_id")));
        }
        if (row.containsKey("building_id") && row.get("building_id") != null) {
            return permissionService.canRead(user, ObjectType.BUILDING, longValue(row.get("building_id")));
        }
        if (row.containsKey("unit_id") && row.get("unit_id") != null) {
            return permissionService.canRead(user, ObjectType.MILITARY_UNIT, longValue(row.get("unit_id")));
        }
        if (row.containsKey("formation_id") && row.get("formation_id") != null) {
            return permissionService.canRead(user, ObjectType.FORMATION, longValue(row.get("formation_id")));
        }
        return true;
    }

    private Map<String, Object> normalize(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        row.forEach((key, value) -> normalized.put(key.toLowerCase(java.util.Locale.ROOT), value));
        return normalized;
    }

    private Map<String, Object> parameters(ExecuteQueryRequest request) {
        return request == null || request.parameters() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request.parameters());
    }

    private ObjectType objectType(String type) {
        String normalized = type.trim().toUpperCase(java.util.Locale.ROOT);
        return "UNIT".equals(normalized) ? ObjectType.MILITARY_UNIT : ObjectType.from(normalized);
    }

    private String preview(QueryTemplate template, ExecuteQueryRequest request) {
        if (request != null && request.previewCommand() != null && !request.previewCommand().isBlank()) {
            return request.previewCommand();
        }
        return template.metadata().exampleCommand();
    }

    private String csvCell(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString().replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private boolean isBlank(Object value) {
        return value == null || value.toString().isBlank();
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString().trim().toUpperCase(java.util.Locale.ROOT);
    }

    private record QueryDefinition(String sql, Map<String, Object> parameters) {
    }
}
