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
            return objectType == ObjectType.PERSONNEL && scope.selfAssignmentIds().contains(objectId);
        }
        if (objectType == ObjectType.SELF) {
            return scope.soldierId().equals(objectId) || scope.selfAssignmentIds().contains(objectId);
        }
        if (objectType == ObjectType.PERSONNEL && (scope.soldierId().equals(objectId) || scope.selfAssignmentIds().contains(objectId))) {
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
        Set<Long> formationIds = scope.formationAssignmentIds();
        if (formationIds.isEmpty()) {
            return false;
        }

        return exists("""
                SELECT EXISTS (
                    SELECT 1
                    FROM v_formation_closure fc
                    WHERE fc.root_formation_id IN (:formationIds)
                      AND fc.descendant_formation_id = :formationId
                )
                """, Map.of(
                "formationId", formationId,
                "formationIds", formationIds
        ));
    }

    private boolean isUnitInScope(UserScope scope, Long unitId) {
        if (scope.unitAssignmentIds().contains(unitId)) {
            return true;
        }
        Set<Long> formationIds = scope.formationAssignmentIds();
        if (formationIds.isEmpty()) {
            return false;
        }
        if (!hasFormationClosure() || !schemaIntrospectionService.relationExists("military_units")) {
            return scope.hasDirectAssignment(ObjectType.MILITARY_UNIT, unitId);
        }

        return exists("""
                SELECT EXISTS (
                    SELECT 1
                    FROM military_units mu
                    JOIN v_formation_closure fc
                        ON fc.descendant_formation_id = mu.formation_id
                    WHERE mu.unit_id = :unitId
                      AND fc.root_formation_id IN (:formationIds)
                )
                """, Map.of(
                "unitId", unitId,
                "formationIds", formationIds
        ));
    }

    private boolean isSubdivisionInScope(UserScope scope, Long subdivisionId) {
        if (isAssignedSubdivisionDescendant(scope, subdivisionId)) {
            return true;
        }
        Long unitId = subdivisionUnitId(subdivisionId);
        if (unitId != null) {
            return isUnitInScope(scope, unitId);
        }
        if (!schemaIntrospectionService.relationExists("subdivisions")) {
            return scope.hasDirectAssignment(ObjectType.BATTALION, subdivisionId)
                    || scope.hasDirectAssignment(ObjectType.COMPANY, subdivisionId)
                    || scope.hasDirectAssignment(ObjectType.PLATOON, subdivisionId)
                    || scope.hasDirectAssignment(ObjectType.SQUAD, subdivisionId);
        }

        return false;
    }

    private boolean isPersonnelInScope(UserScope scope, Long personnelId) {
        if (scope.soldierId().equals(personnelId)) {
            return true;
        }
        if (scope.selfAssignmentIds().contains(personnelId)) {
            return true;
        }
        if (!schemaIntrospectionService.relationExists("personnel")) {
            return false;
        }

        return personnelSubdivisionInScope(scope, personnelId);
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

    private boolean isAssignedSubdivisionDescendant(UserScope scope, Long subdivisionId) {
        Set<Long> subdivisionIds = scope.subdivisionAssignmentIds();
        if (subdivisionIds.isEmpty() || !schemaIntrospectionService.relationExists("subdivisions")) {
            return false;
        }

        return exists("""
                SELECT EXISTS (
                    WITH RECURSIVE assigned_subdivisions AS (
                        SELECT subdivision_id
                        FROM subdivisions
                        WHERE subdivision_id IN (:subdivisionIds)
                        UNION ALL
                        SELECT child.subdivision_id
                        FROM subdivisions child
                        JOIN assigned_subdivisions parent
                            ON child.parent_id = parent.subdivision_id
                    )
                    SELECT 1
                    FROM assigned_subdivisions
                    WHERE subdivision_id = :subdivisionId
                )
                """, Map.of(
                "subdivisionIds", subdivisionIds,
                "subdivisionId", subdivisionId
        ));
    }

    private Long subdivisionUnitId(Long subdivisionId) {
        if (!schemaIntrospectionService.relationExists("subdivisions")) {
            return null;
        }

        return jdbcTemplate.query("""
                SELECT unit_id
                FROM subdivisions
                WHERE subdivision_id = :subdivisionId
                """, Map.of("subdivisionId", subdivisionId), rs -> rs.next() ? rs.getLong("unit_id") : null);
    }

    private boolean hasFormationClosure() {
        return schemaIntrospectionService.relationExists("v_formation_closure");
    }

    private boolean exists(String sql, Map<String, ?> parameters) {
        try {
            return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, parameters, Boolean.class));
        } catch (DataAccessException exception) {
            return false;
        }
    }
}
