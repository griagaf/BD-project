package com.tacticaldistrict.command.hierarchy.repository;

import com.tacticaldistrict.command.hierarchy.dto.CommanderResponse;
import com.tacticaldistrict.command.hierarchy.dto.FormationResponse;
import com.tacticaldistrict.command.hierarchy.dto.ObjectPassportResponse;
import com.tacticaldistrict.command.hierarchy.dto.SubdivisionResponse;
import com.tacticaldistrict.command.hierarchy.dto.TreeNodeDto;
import com.tacticaldistrict.command.hierarchy.dto.TreeNodeMetricsDto;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.unit.dto.UnitResponse;
import com.tacticaldistrict.command.user.service.UserContext;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HierarchyQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<TreeNodeDto> rootNodes(UserContext user) {
        if (user.hasAnyRole(RoleCode.ADMIN_DISTRICT, RoleCode.STAFF_ANALYST)) {
            return jdbcTemplate.query("""
                    SELECT mf.formation_id AS object_id
                    FROM military_formations mf
                    WHERE mf.parent_id IS NULL
                    ORDER BY mf.name
                    """, Map.of(), (rs, rowNum) -> findNode(ObjectType.from("DISTRICT"), rs.getLong("object_id")).orElse(null))
                    .stream()
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }
        if (user.soldierId() == null) {
            return List.of();
        }

        List<TreeNodeDto> assignedNodes = jdbcTemplate.query("""
                SELECT ca.object_type, ca.object_id
                FROM command_assignments ca
                WHERE ca.soldier_id = :soldierId
                  AND ca.starts_at <= CURRENT_DATE
                  AND (ca.ends_at IS NULL OR ca.ends_at >= CURRENT_DATE)
                ORDER BY ca.is_primary DESC, ca.assignment_id
                """, Map.of("soldierId", user.soldierId()), (rs, rowNum) -> {
            String type = rs.getString("object_type");
            Long id = rs.getLong("object_id");
            if ("SELF".equals(type)) {
                return findPersonnelNode(id).orElse(null);
            }
            return findNode(ObjectType.from(type), id).orElse(null);
        });

        return assignedNodes.stream()
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public List<TreeNodeDto> children(ObjectType objectType, Long objectId) {
        return switch (objectType) {
            case DISTRICT, FORMATION, ARMY, CORPS, DIVISION, BRIGADE -> formationChildren(objectId);
            case MILITARY_UNIT -> unitChildren(objectId);
            case BATTALION, COMPANY, PLATOON, SQUAD -> subdivisionChildren(objectId);
            default -> List.of();
        };
    }

    public List<TreeNodeDto> siblings(ObjectType objectType, Long objectId) {
        return switch (objectType) {
            case DISTRICT, FORMATION, ARMY, CORPS, DIVISION, BRIGADE -> formationSiblings(objectId);
            case MILITARY_UNIT -> unitSiblings(objectId);
            case BATTALION, COMPANY, PLATOON, SQUAD -> subdivisionSiblings(objectId);
            default -> List.of();
        };
    }

    public Optional<TreeNodeDto> findNode(ObjectType objectType, Long objectId) {
        return switch (objectType) {
            case DISTRICT, FORMATION, ARMY, CORPS, DIVISION, BRIGADE -> formationNode(objectId);
            case MILITARY_UNIT -> unitNode(objectId);
            case BATTALION, COMPANY, PLATOON, SQUAD -> subdivisionNode(objectId);
            case PERSONNEL, SELF -> findPersonnelNode(objectId);
            default -> Optional.empty();
        };
    }

    public List<TreeNodeDto> breadcrumbPath(ObjectType objectType, Long objectId) {
        return switch (objectType) {
            case DISTRICT, FORMATION, ARMY, CORPS, DIVISION, BRIGADE -> formationPath(objectId);
            case MILITARY_UNIT -> unitPath(objectId);
            case BATTALION, COMPANY, PLATOON, SQUAD -> subdivisionPath(objectId);
            case PERSONNEL, SELF -> personnelPath(objectId);
            default -> List.of();
        };
    }

    public ObjectPassportResponse passport(ObjectType objectType, Long objectId) {
        TreeNodeDto node = findNode(objectType, objectId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Hierarchy object not found: " + objectType + ":" + objectId));
        Map<String, Object> details = details(objectType, objectId);
        return new ObjectPassportResponse(
                node.type(),
                node.objectId(),
                node.label(),
                node.subtitle(),
                node.status(),
                node.commander(),
                node.metrics(),
                breadcrumbPath(objectType, objectId),
                details
        );
    }

    public List<FormationResponse> formations() {
        return jdbcTemplate.query("""
                SELECT mf.formation_id,
                       mf.name,
                       mf.formation_type,
                       mf.parent_id,
                       mf.formation_date,
                       mf.status,
                       mf.commander_id,
                       trim(concat(cp.last_name, ' ', cp.first_name, ' ', coalesce(cp.middle_name, ''))) AS commander_name,
                       mr.name AS rank_name
                FROM military_formations mf
                LEFT JOIN personnel cp ON cp.personnel_id = mf.commander_id
                LEFT JOIN personnel_ranks pr ON pr.personnel_id = cp.personnel_id
                LEFT JOIN military_ranks mr ON mr.rank_id = pr.rank_id
                ORDER BY mf.parent_id NULLS FIRST, mf.name
                """, Map.of(), this::mapFormation);
    }

    public Optional<FormationResponse> formation(Long id) {
        return jdbcTemplate.query("""
                SELECT mf.formation_id,
                       mf.name,
                       mf.formation_type,
                       mf.parent_id,
                       mf.formation_date,
                       mf.status,
                       mf.commander_id,
                       trim(concat(cp.last_name, ' ', cp.first_name, ' ', coalesce(cp.middle_name, ''))) AS commander_name,
                       mr.name AS rank_name
                FROM military_formations mf
                LEFT JOIN personnel cp ON cp.personnel_id = mf.commander_id
                LEFT JOIN personnel_ranks pr ON pr.personnel_id = cp.personnel_id
                LEFT JOIN military_ranks mr ON mr.rank_id = pr.rank_id
                WHERE mf.formation_id = :id
                """, Map.of("id", id), this::mapFormation).stream().findFirst();
    }

    public List<SubdivisionResponse> subdivisions() {
        return jdbcTemplate.query(subdivisionSelect() + " ORDER BY s.unit_id, s.parent_id NULLS FIRST, s.name", Map.of(), this::mapSubdivision);
    }

    public Optional<SubdivisionResponse> subdivision(Long id) {
        return jdbcTemplate.query(subdivisionSelect() + " WHERE s.subdivision_id = :id", Map.of("id", id), this::mapSubdivision)
                .stream()
                .findFirst();
    }

    public List<UnitResponse> units() {
        return jdbcTemplate.query(unitSelect() + " ORDER BY mu.name", Map.of(), this::mapUnit);
    }

    public Optional<UnitResponse> unit(Long id) {
        return jdbcTemplate.query(unitSelect() + " WHERE mu.unit_id = :id", Map.of("id", id), this::mapUnit)
                .stream()
                .findFirst();
    }

    public ObjectType formationObjectType(Long formationId) {
        String type = jdbcTemplate.query("""
                SELECT formation_type
                FROM military_formations
                WHERE formation_id = :id
                """, Map.of("id", formationId), rs -> rs.next() ? rs.getString("formation_type") : null);
        return formationType(type);
    }

    public ObjectType subdivisionObjectType(Long subdivisionId) {
        String type = jdbcTemplate.query("""
                SELECT type
                FROM subdivisions
                WHERE subdivision_id = :id
                """, Map.of("id", subdivisionId), rs -> rs.next() ? rs.getString("type") : null);
        return subdivisionType(type);
    }

    public Optional<Long> unitFormationId(Long unitId) {
        Long value = jdbcTemplate.query("""
                SELECT formation_id
                FROM military_units
                WHERE unit_id = :id
                """, Map.of("id", unitId), rs -> rs.next() ? rs.getLong("formation_id") : null);
        return Optional.ofNullable(value);
    }

    public Optional<Long> subdivisionUnitId(Long subdivisionId) {
        Long value = jdbcTemplate.query("""
                SELECT unit_id
                FROM subdivisions
                WHERE subdivision_id = :id
                """, Map.of("id", subdivisionId), rs -> rs.next() ? rs.getLong("unit_id") : null);
        return Optional.ofNullable(value);
    }

    private List<TreeNodeDto> formationChildren(Long formationId) {
        List<TreeNodeDto> nodes = new ArrayList<>();
        nodes.addAll(jdbcTemplate.query("""
                SELECT formation_id
                FROM military_formations
                WHERE parent_id = :id
                ORDER BY name
                """, Map.of("id", formationId), (rs, rowNum) -> formationNode(rs.getLong("formation_id")).orElse(null)));
        nodes.addAll(jdbcTemplate.query("""
                SELECT unit_id
                FROM military_units
                WHERE formation_id = :id
                ORDER BY name
                """, Map.of("id", formationId), (rs, rowNum) -> unitNode(rs.getLong("unit_id")).orElse(null)));
        return nodes.stream().filter(java.util.Objects::nonNull).toList();
    }

    private List<TreeNodeDto> unitChildren(Long unitId) {
        return jdbcTemplate.query("""
                SELECT subdivision_id
                FROM subdivisions
                WHERE unit_id = :id
                  AND parent_id IS NULL
                ORDER BY name
                """, Map.of("id", unitId), (rs, rowNum) -> subdivisionNode(rs.getLong("subdivision_id")).orElse(null))
                .stream()
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private List<TreeNodeDto> subdivisionChildren(Long subdivisionId) {
        return jdbcTemplate.query("""
                SELECT subdivision_id
                FROM subdivisions
                WHERE parent_id = :id
                ORDER BY name
                """, Map.of("id", subdivisionId), (rs, rowNum) -> subdivisionNode(rs.getLong("subdivision_id")).orElse(null))
                .stream()
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private Optional<TreeNodeDto> formationNode(Long id) {
        return jdbcTemplate.query("""
                SELECT mf.formation_id,
                       mf.name,
                       mf.formation_type,
                       mf.status,
                       mf.commander_id,
                       trim(concat(cp.last_name, ' ', cp.first_name, ' ', coalesce(cp.middle_name, ''))) AS commander_name,
                       mr.name AS rank_name,
                       (SELECT COUNT(*) FROM military_formations child WHERE child.parent_id = mf.formation_id)
                         + (SELECT COUNT(*) FROM military_units mu WHERE mu.formation_id = mf.formation_id) AS children_count,
                       (SELECT COUNT(*) FROM military_units mu JOIN v_formation_closure fc ON fc.descendant_formation_id = mu.formation_id WHERE fc.root_formation_id = mf.formation_id) AS unit_count,
                       (SELECT COUNT(*) FROM personnel p JOIN subdivisions s ON s.subdivision_id = p.subdivision_id JOIN military_units mu ON mu.unit_id = s.unit_id JOIN v_formation_closure fc ON fc.descendant_formation_id = mu.formation_id WHERE fc.root_formation_id = mf.formation_id) AS personnel_count,
                       (SELECT COUNT(*) FROM subdivisions s JOIN military_units mu ON mu.unit_id = s.unit_id JOIN v_formation_closure fc ON fc.descendant_formation_id = mu.formation_id WHERE fc.root_formation_id = mf.formation_id) AS subdivision_count
                FROM military_formations mf
                LEFT JOIN personnel cp ON cp.personnel_id = mf.commander_id
                LEFT JOIN personnel_ranks pr ON pr.personnel_id = cp.personnel_id
                LEFT JOIN military_ranks mr ON mr.rank_id = pr.rank_id
                WHERE mf.formation_id = :id
                """, Map.of("id", id), this::mapFormationNode).stream().findFirst();
    }

    private Optional<TreeNodeDto> unitNode(Long id) {
        return jdbcTemplate.query("""
                SELECT mu.unit_id,
                       mu.name,
                       mf.name AS formation_name,
                       mu.commander_id,
                       trim(concat(cp.last_name, ' ', cp.first_name, ' ', coalesce(cp.middle_name, ''))) AS commander_name,
                       mr.name AS rank_name,
                       (SELECT COUNT(*) FROM subdivisions s WHERE s.unit_id = mu.unit_id AND s.parent_id IS NULL) AS children_count,
                       (SELECT COUNT(*) FROM personnel p JOIN subdivisions s ON s.subdivision_id = p.subdivision_id WHERE s.unit_id = mu.unit_id) AS personnel_count,
                       (SELECT COUNT(*) FROM subdivisions s WHERE s.unit_id = mu.unit_id) AS subdivision_count
                FROM military_units mu
                JOIN military_formations mf ON mf.formation_id = mu.formation_id
                LEFT JOIN personnel cp ON cp.personnel_id = mu.commander_id
                LEFT JOIN personnel_ranks pr ON pr.personnel_id = cp.personnel_id
                LEFT JOIN military_ranks mr ON mr.rank_id = pr.rank_id
                WHERE mu.unit_id = :id
                """, Map.of("id", id), this::mapUnitNode).stream().findFirst();
    }

    private Optional<TreeNodeDto> subdivisionNode(Long id) {
        return jdbcTemplate.query("""
                SELECT s.subdivision_id,
                       s.name,
                       s.type,
                       mu.name AS unit_name,
                       s.commander_id,
                       trim(concat(cp.last_name, ' ', cp.first_name, ' ', coalesce(cp.middle_name, ''))) AS commander_name,
                       mr.name AS rank_name,
                       (SELECT COUNT(*) FROM subdivisions child WHERE child.parent_id = s.subdivision_id) AS children_count,
                       (SELECT COUNT(*) FROM personnel p WHERE p.subdivision_id = s.subdivision_id) AS personnel_count
                FROM subdivisions s
                JOIN military_units mu ON mu.unit_id = s.unit_id
                LEFT JOIN personnel cp ON cp.personnel_id = s.commander_id
                LEFT JOIN personnel_ranks pr ON pr.personnel_id = cp.personnel_id
                LEFT JOIN military_ranks mr ON mr.rank_id = pr.rank_id
                WHERE s.subdivision_id = :id
                """, Map.of("id", id), this::mapSubdivisionNode).stream().findFirst();
    }

    private Optional<TreeNodeDto> findPersonnelNode(Long id) {
        return jdbcTemplate.query("""
                SELECT p.personnel_id,
                       trim(concat(p.last_name, ' ', p.first_name, ' ', coalesce(p.middle_name, ''))) AS full_name,
                       s.name AS subdivision_name,
                       mr.name AS rank_name
                FROM personnel p
                JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
                LEFT JOIN personnel_ranks pr ON pr.personnel_id = p.personnel_id
                LEFT JOIN military_ranks mr ON mr.rank_id = pr.rank_id
                WHERE p.personnel_id = :id
                """, Map.of("id", id), (rs, rowNum) -> new TreeNodeDto(
                "PERSONNEL:" + rs.getLong("personnel_id"),
                "PERSONNEL",
                rs.getLong("personnel_id"),
                rs.getString("full_name"),
                rs.getString("rank_name") == null ? rs.getString("subdivision_name") : rs.getString("rank_name"),
                "ACTIVE",
                9,
                false,
                false,
                0,
                null,
                new TreeNodeMetricsDto(1, 0, 0, 0, 0, 100),
                List.of()
        )).stream().findFirst();
    }

    private List<TreeNodeDto> formationPath(Long formationId) {
        List<Long> ids = jdbcTemplate.query("""
                WITH RECURSIVE path AS (
                    SELECT mf.formation_id, mf.parent_id, 1 AS depth
                    FROM military_formations mf
                    WHERE mf.formation_id = :id
                    UNION ALL
                    SELECT parent.formation_id, parent.parent_id, path.depth + 1
                    FROM military_formations parent
                    JOIN path ON path.parent_id = parent.formation_id
                )
                SELECT formation_id
                FROM path
                ORDER BY depth DESC
                """, Map.of("id", formationId), (rs, rowNum) -> rs.getLong("formation_id"));
        return ids.stream().map(id -> formationNode(id).orElse(null)).filter(java.util.Objects::nonNull).toList();
    }

    private List<TreeNodeDto> unitPath(Long unitId) {
        Long formationId = unitFormationId(unitId).orElse(null);
        List<TreeNodeDto> nodes = new ArrayList<>();
        if (formationId != null) {
            nodes.addAll(formationPath(formationId));
        }
        unitNode(unitId).ifPresent(nodes::add);
        return nodes;
    }

    private List<TreeNodeDto> subdivisionPath(Long subdivisionId) {
        Map<Long, TreeNodeDto> ordered = new LinkedHashMap<>();
        Long unitId = subdivisionUnitId(subdivisionId).orElse(null);
        if (unitId != null) {
            unitPath(unitId).forEach(node -> ordered.put(node.objectId(), node));
        }
        List<Long> ids = jdbcTemplate.query("""
                WITH RECURSIVE path AS (
                    SELECT s.subdivision_id, s.parent_id, 1 AS depth
                    FROM subdivisions s
                    WHERE s.subdivision_id = :id
                    UNION ALL
                    SELECT parent.subdivision_id, parent.parent_id, path.depth + 1
                    FROM subdivisions parent
                    JOIN path ON path.parent_id = parent.subdivision_id
                )
                SELECT subdivision_id
                FROM path
                ORDER BY depth DESC
                """, Map.of("id", subdivisionId), (rs, rowNum) -> rs.getLong("subdivision_id"));
        ids.stream().map(id -> subdivisionNode(id).orElse(null)).filter(java.util.Objects::nonNull)
                .forEach(node -> ordered.put(-node.objectId(), node));
        return new ArrayList<>(ordered.values());
    }

    private List<TreeNodeDto> personnelPath(Long personnelId) {
        Long subdivisionId = jdbcTemplate.query("""
                SELECT subdivision_id
                FROM personnel
                WHERE personnel_id = :id
                """, Map.of("id", personnelId), rs -> rs.next() ? rs.getLong("subdivision_id") : null);
        List<TreeNodeDto> nodes = new ArrayList<>();
        if (subdivisionId != null) {
            nodes.addAll(subdivisionPath(subdivisionId));
        }
        findPersonnelNode(personnelId).ifPresent(nodes::add);
        return nodes;
    }

    private List<TreeNodeDto> formationSiblings(Long formationId) {
        Long parentId = jdbcTemplate.query("SELECT parent_id FROM military_formations WHERE formation_id = :id",
                Map.of("id", formationId), rs -> rs.next() ? rs.getObject("parent_id", Long.class) : null);
        String sql = parentId == null
                ? "SELECT formation_id FROM military_formations WHERE parent_id IS NULL ORDER BY name"
                : "SELECT formation_id FROM military_formations WHERE parent_id = :parentId ORDER BY name";
        Map<String, Object> params = new HashMap<>();
        if (parentId != null) {
            params.put("parentId", parentId);
        }
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> formationNode(rs.getLong("formation_id")).orElse(null))
                .stream().filter(java.util.Objects::nonNull).toList();
    }

    private List<TreeNodeDto> unitSiblings(Long unitId) {
        Long formationId = unitFormationId(unitId).orElse(null);
        if (formationId == null) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT unit_id
                FROM military_units
                WHERE formation_id = :formationId
                ORDER BY name
                """, Map.of("formationId", formationId), (rs, rowNum) -> unitNode(rs.getLong("unit_id")).orElse(null))
                .stream().filter(java.util.Objects::nonNull).toList();
    }

    private List<TreeNodeDto> subdivisionSiblings(Long subdivisionId) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", subdivisionId);
        SubdivisionRelation relation = jdbcTemplate.query("""
                SELECT unit_id, parent_id
                FROM subdivisions
                WHERE subdivision_id = :id
                """, params, rs -> rs.next()
                ? new SubdivisionRelation(rs.getLong("unit_id"), rs.getObject("parent_id", Long.class))
                : null);
        if (relation == null) {
            return List.of();
        }
        String sql = relation.parentId() == null
                ? "SELECT subdivision_id FROM subdivisions WHERE unit_id = :unitId AND parent_id IS NULL ORDER BY name"
                : "SELECT subdivision_id FROM subdivisions WHERE parent_id = :parentId ORDER BY name";
        Map<String, Object> siblingParams = new HashMap<>();
        siblingParams.put("unitId", relation.unitId());
        if (relation.parentId() != null) {
            siblingParams.put("parentId", relation.parentId());
        }
        return jdbcTemplate.query(sql, siblingParams, (rs, rowNum) -> subdivisionNode(rs.getLong("subdivision_id")).orElse(null))
                .stream().filter(java.util.Objects::nonNull).toList();
    }

    private Map<String, Object> details(ObjectType objectType, Long objectId) {
        Map<String, Object> details = new LinkedHashMap<>();
        switch (objectType) {
            case DISTRICT, FORMATION, ARMY, CORPS, DIVISION, BRIGADE -> formation(objectId).ifPresent(value -> {
                details.put("formationType", value.formationType());
                details.put("formationDate", value.formationDate());
                details.put("parentId", value.parentId());
            });
            case MILITARY_UNIT -> unit(objectId).ifPresent(value -> {
                details.put("formationId", value.formationId());
                details.put("formationName", value.formationName());
                details.put("locationName", value.locationName());
            });
            case BATTALION, COMPANY, PLATOON, SQUAD -> subdivision(objectId).ifPresent(value -> {
                details.put("subdivisionType", value.type());
                details.put("unitId", value.unitId());
                details.put("parentId", value.parentId());
            });
            default -> {
            }
        }
        return details;
    }

    private TreeNodeDto mapFormationNode(ResultSet rs, int rowNum) throws SQLException {
        ObjectType type = formationType(rs.getString("formation_type"));
        int childrenCount = rs.getInt("children_count");
        int personnelCount = rs.getInt("personnel_count");
        int unitCount = rs.getInt("unit_count");
        int subdivisionCount = rs.getInt("subdivision_count");
        return new TreeNodeDto(
                type.name() + ":" + rs.getLong("formation_id"),
                type.name(),
                rs.getLong("formation_id"),
                rs.getString("name"),
                rs.getString("formation_type"),
                rs.getString("status"),
                formationLevel(type),
                childrenCount > 0,
                false,
                childrenCount,
                commander(rs.getObject("commander_id", Long.class), rs.getString("commander_name"), rs.getString("rank_name")),
                metrics(personnelCount, unitCount, subdivisionCount),
                List.of()
        );
    }

    private TreeNodeDto mapUnitNode(ResultSet rs, int rowNum) throws SQLException {
        int childrenCount = rs.getInt("children_count");
        int personnelCount = rs.getInt("personnel_count");
        int subdivisionCount = rs.getInt("subdivision_count");
        return new TreeNodeDto(
                "MILITARY_UNIT:" + rs.getLong("unit_id"),
                "MILITARY_UNIT",
                rs.getLong("unit_id"),
                rs.getString("name"),
                rs.getString("formation_name"),
                "ACTIVE",
                5,
                childrenCount > 0,
                false,
                childrenCount,
                commander(rs.getObject("commander_id", Long.class), rs.getString("commander_name"), rs.getString("rank_name")),
                metrics(personnelCount, 1, subdivisionCount),
                List.of()
        );
    }

    private TreeNodeDto mapSubdivisionNode(ResultSet rs, int rowNum) throws SQLException {
        ObjectType type = subdivisionType(rs.getString("type"));
        int childrenCount = rs.getInt("children_count");
        int personnelCount = rs.getInt("personnel_count");
        return new TreeNodeDto(
                type.name() + ":" + rs.getLong("subdivision_id"),
                type.name(),
                rs.getLong("subdivision_id"),
                rs.getString("name"),
                rs.getString("type") + " / " + rs.getString("unit_name"),
                "ACTIVE",
                subdivisionLevel(type),
                childrenCount > 0,
                false,
                childrenCount,
                commander(rs.getObject("commander_id", Long.class), rs.getString("commander_name"), rs.getString("rank_name")),
                metrics(personnelCount, 0, 1),
                List.of()
        );
    }

    private FormationResponse mapFormation(ResultSet rs, int rowNum) throws SQLException {
        return new FormationResponse(
                rs.getLong("formation_id"),
                rs.getString("name"),
                rs.getString("formation_type"),
                rs.getObject("parent_id", Long.class),
                rs.getObject("formation_date", LocalDate.class),
                rs.getString("status"),
                commander(rs.getObject("commander_id", Long.class), rs.getString("commander_name"), rs.getString("rank_name"))
        );
    }

    private UnitResponse mapUnit(ResultSet rs, int rowNum) throws SQLException {
        return new UnitResponse(
                rs.getLong("unit_id"),
                rs.getString("name"),
                rs.getLong("formation_id"),
                rs.getString("formation_name"),
                rs.getObject("location_id", Long.class),
                rs.getString("location_name"),
                commander(rs.getObject("commander_id", Long.class), rs.getString("commander_name"), rs.getString("rank_name"))
        );
    }

    private SubdivisionResponse mapSubdivision(ResultSet rs, int rowNum) throws SQLException {
        return new SubdivisionResponse(
                rs.getLong("subdivision_id"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getLong("unit_id"),
                rs.getObject("parent_id", Long.class),
                commander(rs.getObject("commander_id", Long.class), rs.getString("commander_name"), rs.getString("rank_name"))
        );
    }

    private String unitSelect() {
        return """
                SELECT mu.unit_id,
                       mu.name,
                       mu.formation_id,
                       mf.name AS formation_name,
                       mu.location_id,
                       l.city AS location_name,
                       mu.commander_id,
                       trim(concat(cp.last_name, ' ', cp.first_name, ' ', coalesce(cp.middle_name, ''))) AS commander_name,
                       mr.name AS rank_name
                FROM military_units mu
                JOIN military_formations mf ON mf.formation_id = mu.formation_id
                LEFT JOIN locations l ON l.location_id = mu.location_id
                LEFT JOIN personnel cp ON cp.personnel_id = mu.commander_id
                LEFT JOIN personnel_ranks pr ON pr.personnel_id = cp.personnel_id
                LEFT JOIN military_ranks mr ON mr.rank_id = pr.rank_id
                """;
    }

    private String subdivisionSelect() {
        return """
                SELECT s.subdivision_id,
                       s.name,
                       s.type,
                       s.unit_id,
                       s.parent_id,
                       s.commander_id,
                       trim(concat(cp.last_name, ' ', cp.first_name, ' ', coalesce(cp.middle_name, ''))) AS commander_name,
                       mr.name AS rank_name
                FROM subdivisions s
                LEFT JOIN personnel cp ON cp.personnel_id = s.commander_id
                LEFT JOIN personnel_ranks pr ON pr.personnel_id = cp.personnel_id
                LEFT JOIN military_ranks mr ON mr.rank_id = pr.rank_id
                """;
    }

    private CommanderResponse commander(Long id, String fullName, String rankName) {
        if (id == null) {
            return null;
        }
        return new CommanderResponse(id, fullName == null || fullName.isBlank() ? "Assigned commander" : fullName, rankName);
    }

    private TreeNodeMetricsDto metrics(int personnelCount, int unitCount, int subdivisionCount) {
        int readiness = Math.min(100, 45 + Math.min(personnelCount, 40) + Math.min(unitCount + subdivisionCount, 15));
        return new TreeNodeMetricsDto(personnelCount, unitCount, subdivisionCount, 0, 0, readiness);
    }

    public static ObjectType formationType(String value) {
        return switch (value) {
            case "Округ" -> ObjectType.DISTRICT;
            case "Армия" -> ObjectType.ARMY;
            case "Корпус" -> ObjectType.CORPS;
            case "Дивизия" -> ObjectType.DIVISION;
            case "Бригада" -> ObjectType.BRIGADE;
            default -> ObjectType.FORMATION;
        };
    }

    public static ObjectType subdivisionType(String value) {
        return switch (value) {
            case "Батальон" -> ObjectType.BATTALION;
            case "Рота" -> ObjectType.COMPANY;
            case "Взвод" -> ObjectType.PLATOON;
            case "Отделение" -> ObjectType.SQUAD;
            default -> ObjectType.PLATOON;
        };
    }

    private int formationLevel(ObjectType type) {
        return switch (type) {
            case DISTRICT -> 1;
            case ARMY -> 2;
            case CORPS, DIVISION, BRIGADE, FORMATION -> 3;
            default -> 3;
        };
    }

    private int subdivisionLevel(ObjectType type) {
        return switch (type) {
            case BATTALION -> 6;
            case COMPANY -> 7;
            case PLATOON -> 8;
            case SQUAD -> 9;
            default -> 8;
        };
    }

    private record SubdivisionRelation(Long unitId, Long parentId) {
    }
}
