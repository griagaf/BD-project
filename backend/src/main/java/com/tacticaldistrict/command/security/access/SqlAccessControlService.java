package com.tacticaldistrict.command.security.access;

import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.user.service.UserContext;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SqlAccessControlService implements AccessControlService {

    private static final Set<ObjectType> FORMATION_ASSIGNMENT_TYPES = Set.of(
            ObjectType.DISTRICT,
            ObjectType.FORMATION,
            ObjectType.ARMY,
            ObjectType.CORPS,
            ObjectType.DIVISION,
            ObjectType.BRIGADE
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final UserScopeResolver userScopeResolver;
    private final SchemaIntrospectionService schemaIntrospectionService;

    @Override
    @Transactional(readOnly = true)
    public boolean isInScope(UserContext user, ObjectType objectType, Long objectId) {
        if (objectType == null || objectId == null) {
            return false;
        }

        UserScope scope = userScopeResolver.resolve(user);
        if (scope.unrestricted()) {
            return true;
        }
        if (scope.soldierId() == null) {
            return false;
        }
        if (objectType == ObjectType.SELF) {
            return scope.soldierId().equals(objectId);
        }
        if (objectType == ObjectType.PERSONNEL && scope.soldierId().equals(objectId)) {
            return true;
        }
        if (scope.hasDirectAssignment(objectType, objectId)) {
            return true;
        }
        if (scope.hasDistrictAssignment()) {
            return true;
        }

        return switch (objectType) {
            case FORMATION, ARMY, CORPS, DIVISION, BRIGADE -> isFormationInScope(scope, objectId);
            case MILITARY_UNIT -> isUnitInScope(scope, objectId);
            case BATTALION, COMPANY, PLATOON, SQUAD -> isSubdivisionInScope(scope, objectId);
            case PERSONNEL -> isPersonnelInScope(scope, objectId);
            case EQUIPMENT, WEAPON -> isUnitInScope(scope, objectId);
            case BUILDING -> isBuildingInScope(scope, objectId);
            default -> false;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> filterIdsInScope(UserContext user, ObjectType objectType, Collection<Long> objectIds) {
        if (objectIds == null || objectIds.isEmpty()) {
            return Set.of();
        }

        Set<Long> visibleIds = new LinkedHashSet<>();
        for (Long objectId : objectIds) {
            if (isInScope(user, objectType, objectId)) {
                visibleIds.add(objectId);
            }
        }
        return visibleIds;
    }

    @Override
    public ScopedQuery applyDirectScopeFilter(UserContext user, String sql, ObjectType objectType, String idColumn) {
        UserScope scope = userScopeResolver.resolve(user);
        if (scope.unrestricted()) {
            return new ScopedQuery(sql, Map.of());
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("scopeSoldierId", scope.soldierId());
        parameters.put("scopeObjectType", objectType.name());

        String scopedSql = """
                SELECT scoped_result.*
                FROM (
                %s
                ) scoped_result
                WHERE (
                    (:scopeSoldierId IS NOT NULL AND :scopeObjectType = 'PERSONNEL' AND scoped_result.%s = :scopeSoldierId)
                    OR EXISTS (
                        SELECT 1
                        FROM command_assignments ca
                        WHERE ca.soldier_id = :scopeSoldierId
                          AND ca.starts_at <= CURRENT_DATE
                          AND (ca.ends_at IS NULL OR ca.ends_at >= CURRENT_DATE)
                          AND ca.object_type = :scopeObjectType
                          AND ca.object_id = scoped_result.%s
                    )
                )
                """.formatted(sql, idColumn, idColumn);

        return new ScopedQuery(scopedSql, parameters);
    }

    private boolean isFormationInScope(UserScope scope, Long formationId) {
        if (!hasFormationClosure()) {
            return scope.hasDirectAssignment(ObjectType.FORMATION, formationId)
                    || scope.hasDirectAssignment(ObjectType.ARMY, formationId)
                    || scope.hasDirectAssignment(ObjectType.CORPS, formationId)
                    || scope.hasDirectAssignment(ObjectType.DIVISION, formationId)
                    || scope.hasDirectAssignment(ObjectType.BRIGADE, formationId);
        }

        return exists("""
                SELECT EXISTS (
                    SELECT 1
                    FROM command_assignments ca
                    JOIN v_formation_closure fc
                        ON fc.root_formation_id = ca.object_id
                    WHERE ca.soldier_id = :soldierId
                      AND ca.starts_at <= CURRENT_DATE
                      AND (ca.ends_at IS NULL OR ca.ends_at >= CURRENT_DATE)
                      AND ca.object_type IN (:formationAssignmentTypes)
                      AND fc.descendant_formation_id = :formationId
                )
                """, Map.of(
                "soldierId", scope.soldierId(),
                "formationId", formationId,
                "formationAssignmentTypes", formationAssignmentTypes()
        ));
    }

    private boolean isUnitInScope(UserScope scope, Long unitId) {
        if (!hasFormationClosure() || !schemaIntrospectionService.relationExists("military_units")) {
            return scope.hasDirectAssignment(ObjectType.MILITARY_UNIT, unitId);
        }

        return exists("""
                SELECT EXISTS (
                    SELECT 1
                    FROM military_units mu
                    JOIN command_assignments ca ON ca.soldier_id = :soldierId
                    LEFT JOIN v_formation_closure fc
                        ON fc.descendant_formation_id = mu.formation_id
                    WHERE mu.unit_id = :unitId
                      AND ca.starts_at <= CURRENT_DATE
                      AND (ca.ends_at IS NULL OR ca.ends_at >= CURRENT_DATE)
                      AND (
                          (ca.object_type = 'MILITARY_UNIT' AND ca.object_id = mu.unit_id)
                          OR (ca.object_type IN (:formationAssignmentTypes)
                              AND ca.object_id = fc.root_formation_id)
                      )
                )
                """, Map.of(
                "soldierId", scope.soldierId(),
                "unitId", unitId,
                "formationAssignmentTypes", formationAssignmentTypes()
        ));
    }

    private boolean isSubdivisionInScope(UserScope scope, Long subdivisionId) {
        if (!schemaIntrospectionService.relationExists("subdivisions")) {
            return scope.hasDirectAssignment(ObjectType.BATTALION, subdivisionId)
                    || scope.hasDirectAssignment(ObjectType.COMPANY, subdivisionId)
                    || scope.hasDirectAssignment(ObjectType.PLATOON, subdivisionId)
                    || scope.hasDirectAssignment(ObjectType.SQUAD, subdivisionId);
        }

        return exists("""
                SELECT EXISTS (
                    WITH RECURSIVE assigned_subdivisions AS (
                        SELECT ca.object_id AS subdivision_id
                        FROM command_assignments ca
                        WHERE ca.soldier_id = :soldierId
                          AND ca.starts_at <= CURRENT_DATE
                          AND (ca.ends_at IS NULL OR ca.ends_at >= CURRENT_DATE)
                          AND ca.object_type IN ('BATTALION', 'COMPANY', 'PLATOON', 'SQUAD')
                        UNION ALL
                        SELECT child.subdivision_id
                        FROM subdivisions child
                        JOIN assigned_subdivisions parent
                            ON child.parent_id = parent.subdivision_id
                    )
                    SELECT 1
                    FROM subdivisions s
                    JOIN military_units mu ON mu.unit_id = s.unit_id
                    JOIN command_assignments ca ON ca.soldier_id = :soldierId
                    LEFT JOIN v_formation_closure fc
                        ON fc.descendant_formation_id = mu.formation_id
                    WHERE s.subdivision_id = :subdivisionId
                      AND ca.starts_at <= CURRENT_DATE
                      AND (ca.ends_at IS NULL OR ca.ends_at >= CURRENT_DATE)
                      AND (
                          s.subdivision_id IN (SELECT subdivision_id FROM assigned_subdivisions)
                          OR (ca.object_type = 'MILITARY_UNIT' AND ca.object_id = mu.unit_id)
                          OR (ca.object_type IN (:formationAssignmentTypes)
                              AND ca.object_id = fc.root_formation_id)
                      )
                )
                """, Map.of(
                "soldierId", scope.soldierId(),
                "subdivisionId", subdivisionId,
                "formationAssignmentTypes", formationAssignmentTypes()
        ));
    }

    private boolean isPersonnelInScope(UserScope scope, Long personnelId) {
        if (scope.soldierId().equals(personnelId)) {
            return true;
        }
        if (!schemaIntrospectionService.relationExists("personnel")) {
            return false;
        }

        return exists("""
                SELECT EXISTS (
                    SELECT 1
                    FROM personnel p
                    WHERE p.personnel_id = :personnelId
                      AND (
                          p.personnel_id = :soldierId
                          OR EXISTS (
                              SELECT 1
                              FROM subdivisions s
                              WHERE s.subdivision_id = p.subdivision_id
                                AND (
                                    :subdivisionInScope = TRUE
                                )
                          )
                      )
                )
                """, Map.of(
                "soldierId", scope.soldierId(),
                "personnelId", personnelId,
                "subdivisionInScope", personnelSubdivisionInScope(scope, personnelId)
        ));
    }

    private boolean personnelSubdivisionInScope(UserScope scope, Long personnelId) {
        if (!schemaIntrospectionService.relationExists("personnel")) {
            return false;
        }

        Long subdivisionId = jdbcTemplate.query("""
                SELECT p.subdivision_id
                FROM personnel p
                WHERE p.personnel_id = :personnelId
                """, Map.of("personnelId", personnelId), rs -> rs.next() ? rs.getLong("subdivision_id") : null);

        return subdivisionId != null && isSubdivisionInScope(scope, subdivisionId);
    }

    private boolean isBuildingInScope(UserScope scope, Long buildingId) {
        if (!schemaIntrospectionService.relationExists("buildings")) {
            return false;
        }

        return exists("""
                SELECT EXISTS (
                    SELECT 1
                    FROM buildings b
                    WHERE b.building_id = :buildingId
                      AND b.unit_id IS NOT NULL
                )
                """, Map.of("buildingId", buildingId))
                && buildingUnitInScope(scope, buildingId);
    }

    private boolean buildingUnitInScope(UserScope scope, Long buildingId) {
        Long unitId = jdbcTemplate.query("""
                SELECT b.unit_id
                FROM buildings b
                WHERE b.building_id = :buildingId
                """, Map.of("buildingId", buildingId), rs -> rs.next() ? rs.getLong("unit_id") : null);

        return unitId != null && isUnitInScope(scope, unitId);
    }

    private boolean hasFormationClosure() {
        return schemaIntrospectionService.relationExists("v_formation_closure");
    }

    private Set<String> formationAssignmentTypes() {
        return FORMATION_ASSIGNMENT_TYPES.stream()
                .map(ObjectType::name)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private boolean exists(String sql, Map<String, ?> parameters) {
        try {
            return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, parameters, Boolean.class));
        } catch (DataAccessException exception) {
            return false;
        }
    }
}
