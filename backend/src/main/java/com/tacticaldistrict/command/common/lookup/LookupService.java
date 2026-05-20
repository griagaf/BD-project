package com.tacticaldistrict.command.common.lookup;

import com.tacticaldistrict.command.security.access.PermissionService;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LookupService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final UserContextProvider userContextProvider;
    private final PermissionService permissionService;

    @Transactional(readOnly = true)
    public List<LookupOptionResponse> units(String search, int limit) {
        UserContext user = userContextProvider.current();
        return jdbcTemplate.query("""
                SELECT mu.unit_id AS id, mu.name AS label, mf.name AS parent_label,
                       concat(
                           (SELECT string_agg(fc.root_formation_name, ' → ' ORDER BY fc.depth DESC)
                            FROM v_formation_closure fc
                            WHERE fc.descendant_formation_id = mu.formation_id),
                           ' → ', mu.name
                       ) AS hierarchy_path,
                       l.city AS location_name
                FROM military_units mu
                JOIN military_formations mf ON mf.formation_id = mu.formation_id
                LEFT JOIN locations l ON l.location_id = mu.location_id
                WHERE (:search = '' OR lower(mu.name) LIKE :pattern OR lower(mf.name) LIKE :pattern
                       OR lower(coalesce(l.city, '')) LIKE :pattern)
                ORDER BY mu.name
                LIMIT :limit
                """, params(search, limit), (rs, rowNum) -> new LookupOptionResponse(
                rs.getLong("id"),
                rs.getString("label"),
                rs.getString("hierarchy_path"),
                ObjectType.MILITARY_UNIT.name(),
                rs.getString("parent_label") + (rs.getString("location_name") == null ? "" : " / " + rs.getString("location_name")),
                permissionService.canRead(user, ObjectType.MILITARY_UNIT, rs.getLong("id")) ? null : "OUT_OF_SCOPE"
        )).stream().filter(option -> option.disabledReason() == null).toList();
    }

    @Transactional(readOnly = true)
    public List<LookupOptionResponse> formations(String search, String types, int limit) {
        UserContext user = userContextProvider.current();
        Map<String, Object> params = params(search, limit);
        List<String> typeList = types == null || types.isBlank()
                ? List.of("Армия", "Корпус", "Дивизия", "Бригада")
                : List.of(types.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::formationDbType)
                .toList();
        params.put("types", typeList);
        return jdbcTemplate.query("""
                SELECT mf.formation_id AS id, mf.name AS label, mf.formation_type, parent.name AS parent_label,
                       (SELECT string_agg(fc.root_formation_name, ' → ' ORDER BY fc.depth DESC)
                        FROM v_formation_closure fc
                        WHERE fc.descendant_formation_id = mf.formation_id) AS hierarchy_path
                FROM military_formations mf
                LEFT JOIN military_formations parent ON parent.formation_id = mf.parent_id
                WHERE mf.formation_type IN (:types)
                  AND (:search = '' OR lower(mf.name) LIKE :pattern OR lower(coalesce(parent.name, '')) LIKE :pattern)
                ORDER BY mf.formation_type, mf.name
                LIMIT :limit
                """, params, (rs, rowNum) -> {
            ObjectType type = formationObjectType(rs.getString("formation_type"));
            Long id = rs.getLong("id");
            return new LookupOptionResponse(
                    id,
                    rs.getString("label"),
                    rs.getString("hierarchy_path"),
                    type.name(),
                    rs.getString("parent_label"),
                    permissionService.canRead(user, type, id) ? null : "OUT_OF_SCOPE"
            );
        }).stream().filter(option -> option.disabledReason() == null).toList();
    }

    @Transactional(readOnly = true)
    public List<LookupOptionResponse> subdivisions(String search, int limit) {
        UserContext user = userContextProvider.current();
        return jdbcTemplate.query("""
                SELECT s.subdivision_id AS id, s.name AS label, s.type, mu.name AS parent_label,
                       concat(mu.name, ' / ', coalesce(parent.name || ' / ', ''), s.name) AS hierarchy_path
                FROM subdivisions s
                JOIN military_units mu ON mu.unit_id = s.unit_id
                LEFT JOIN subdivisions parent ON parent.subdivision_id = s.parent_id
                WHERE (:search = '' OR lower(s.name) LIKE :pattern OR lower(mu.name) LIKE :pattern)
                ORDER BY mu.name, s.type, s.name
                LIMIT :limit
                """, params(search, limit), (rs, rowNum) -> {
            ObjectType type = subdivisionObjectType(rs.getString("type"));
            Long id = rs.getLong("id");
            return new LookupOptionResponse(
                    id,
                    rs.getString("label"),
                    rs.getString("hierarchy_path"),
                    type.name(),
                    rs.getString("parent_label") + " / " + rs.getString("type"),
                    permissionService.canRead(user, type, id) ? null : "OUT_OF_SCOPE"
            );
        }).stream().filter(option -> option.disabledReason() == null).toList();
    }

    @Transactional(readOnly = true)
    public List<LookupOptionResponse> ranks(String search, int limit) {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("personnel:read")) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT rank_id AS id, name AS label
                FROM military_ranks
                WHERE (:search = '' OR lower(name) LIKE :pattern)
                ORDER BY rank_order, name
                LIMIT :limit
                """, params(search, limit), this::simple);
    }

    @Transactional(readOnly = true)
    public List<LookupOptionResponse> specialties(String search, int limit) {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("specialty:read") && !user.hasPermission("personnel:read")) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT specialty_id AS id, name AS label
                FROM specialties
                WHERE (:search = '' OR lower(name) LIKE :pattern)
                ORDER BY name
                LIMIT :limit
                """, params(search, limit), this::simple);
    }

    @Transactional(readOnly = true)
    public List<LookupOptionResponse> equipmentTypes(String search, int limit) {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("equipment:read")) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT et.type_id AS id, et.name AS label, ec.name AS parent_label
                FROM equipment_types et
                JOIN equipment_categories ec ON ec.category_id = et.category_id
                WHERE et.archived = FALSE
                  AND ec.archived = FALSE
                  AND (:search = '' OR lower(et.name) LIKE :pattern OR lower(ec.name) LIKE :pattern)
                ORDER BY ec.name, et.name
                LIMIT :limit
                """, params(search, limit), (rs, rowNum) -> new LookupOptionResponse(
                rs.getLong("id"),
                rs.getString("label"),
                null,
                ObjectType.EQUIPMENT.name(),
                rs.getString("parent_label"),
                null
        ));
    }

    @Transactional(readOnly = true)
    public List<LookupOptionResponse> weaponTypes(String search, int limit) {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("weapon:read")) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT wt.type_id AS id, wt.name AS label, wc.name AS parent_label
                FROM weapon_types wt
                JOIN weapon_categories wc ON wc.category_id = wt.category_id
                WHERE wt.archived = FALSE
                  AND wc.archived = FALSE
                  AND (:search = '' OR lower(wt.name) LIKE :pattern OR lower(wc.name) LIKE :pattern)
                ORDER BY wc.name, wt.name
                LIMIT :limit
                """, params(search, limit), (rs, rowNum) -> new LookupOptionResponse(
                rs.getLong("id"),
                rs.getString("label"),
                null,
                ObjectType.WEAPON.name(),
                rs.getString("parent_label"),
                null
        ));
    }

    @Transactional(readOnly = true)
    public List<LookupOptionResponse> buildings(String search, int limit) {
        UserContext user = userContextProvider.current();
        return jdbcTemplate.query("""
                SELECT b.building_id AS id, b.name AS label, mu.name AS parent_label
                FROM buildings b
                JOIN military_units mu ON mu.unit_id = b.unit_id
                WHERE (:search = '' OR lower(b.name) LIKE :pattern OR lower(mu.name) LIKE :pattern)
                ORDER BY b.name
                LIMIT :limit
                """, params(search, limit), (rs, rowNum) -> new LookupOptionResponse(
                rs.getLong("id"),
                rs.getString("label"),
                null,
                ObjectType.BUILDING.name(),
                rs.getString("parent_label"),
                permissionService.canRead(user, ObjectType.BUILDING, rs.getLong("id")) ? null : "OUT_OF_SCOPE"
        )).stream().filter(option -> option.disabledReason() == null).toList();
    }

    @Transactional(readOnly = true)
    public List<LookupOptionResponse> personnel(String search, int limit) {
        UserContext user = userContextProvider.current();
        return jdbcTemplate.query("""
                SELECT p.personnel_id AS id,
                       trim(p.last_name || ' ' || p.first_name || ' ' || coalesce(p.middle_name, '')) AS label,
                       mr.name AS rank_name,
                       s.name AS parent_label,
                       concat(mu.name, ' / ', s.name) AS hierarchy_path
                FROM personnel p
                JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
                JOIN military_units mu ON mu.unit_id = s.unit_id
                LEFT JOIN personnel_ranks pr ON pr.personnel_id = p.personnel_id
                LEFT JOIN military_ranks mr ON mr.rank_id = pr.rank_id
                WHERE (:search = '' OR lower(p.last_name || ' ' || p.first_name || ' ' || coalesce(p.middle_name, '')) LIKE :pattern
                       OR lower(p.personal_number) LIKE :pattern)
                ORDER BY p.last_name, p.first_name, p.middle_name
                LIMIT :limit
                """, params(search, limit), (rs, rowNum) -> new LookupOptionResponse(
                rs.getLong("id"),
                rs.getString("label"),
                rs.getString("rank_name"),
                ObjectType.PERSONNEL.name(),
                rs.getString("hierarchy_path"),
                permissionService.canRead(user, ObjectType.PERSONNEL, rs.getLong("id")) ? null : "OUT_OF_SCOPE"
        )).stream().filter(option -> option.disabledReason() == null).toList();
    }

    private Map<String, Object> params(String search, int limit) {
        String normalized = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        Map<String, Object> params = new HashMap<>();
        params.put("search", normalized);
        params.put("pattern", "%" + normalized + "%");
        params.put("limit", Math.max(1, Math.min(limit, 100)));
        return params;
    }

    private LookupOptionResponse simple(ResultSet rs, int rowNum) throws SQLException {
        return new LookupOptionResponse(
                rs.getLong("id"),
                rs.getString("label"),
                null,
                null,
                null,
                null
        );
    }

    private String formationDbType(String type) {
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "ARMY" -> "Армия";
            case "CORPS" -> "Корпус";
            case "DIVISION" -> "Дивизия";
            case "BRIGADE" -> "Бригада";
            case "DISTRICT" -> "Округ";
            default -> type;
        };
    }

    private ObjectType formationObjectType(String type) {
        return switch (type) {
            case "Округ" -> ObjectType.DISTRICT;
            case "Армия" -> ObjectType.ARMY;
            case "Корпус" -> ObjectType.CORPS;
            case "Дивизия" -> ObjectType.DIVISION;
            case "Бригада" -> ObjectType.BRIGADE;
            default -> ObjectType.FORMATION;
        };
    }

    private ObjectType subdivisionObjectType(String type) {
        return switch (type) {
            case "Батальон" -> ObjectType.BATTALION;
            case "Рота" -> ObjectType.COMPANY;
            case "Взвод" -> ObjectType.PLATOON;
            case "Отделение" -> ObjectType.SQUAD;
            default -> ObjectType.SQUAD;
        };
    }
}
