package com.tacticaldistrict.command.building.service;

import com.tacticaldistrict.command.audit.AuditService;
import com.tacticaldistrict.command.building.dto.BuildingAssignmentResponse;
import com.tacticaldistrict.command.building.dto.BuildingFilter;
import com.tacticaldistrict.command.building.dto.BuildingRequest;
import com.tacticaldistrict.command.building.dto.BuildingResponse;
import com.tacticaldistrict.command.building.dto.BuildingStatisticsResponse;
import com.tacticaldistrict.command.common.dto.PageResponse;
import com.tacticaldistrict.command.security.access.PermissionService;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import jakarta.persistence.EntityNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BuildingService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final UserContextProvider userContextProvider;
    private final PermissionService permissionService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<BuildingResponse> search(BuildingFilter filter, Pageable pageable) {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("building:read")) {
            throw new AccessDeniedException("Access denied");
        }
        List<BuildingResponse> rows = queryBuildings(filter)
                .stream()
                .filter(row -> permissionService.canRead(user, ObjectType.BUILDING, row.id()))
                .toList();
        return page(rows, pageable);
    }

    @Transactional(readOnly = true)
    public BuildingResponse findById(Long id) {
        UserContext user = userContextProvider.current();
        permissionService.checkRead(user, ObjectType.BUILDING, id);
        return queryBuildings(new BuildingFilter(null, null))
                .stream()
                .filter(row -> row.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Building not found: " + id));
    }

    @Transactional
    public BuildingResponse create(BuildingRequest request) {
        UserContext user = userContextProvider.current();
        permissionService.checkCreate(user, ObjectType.MILITARY_UNIT, request.unitId(), ObjectType.BUILDING);
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO buildings (name, unit_id, assignable)
                VALUES (:name, :unitId, :assignable)
                RETURNING building_id
                """, Map.of(
                "name", request.name(),
                "unitId", request.unitId(),
                "assignable", assignable(request)
        ), Long.class);
        auditService.created(user, ObjectType.BUILDING, id, "Building created");
        return findById(id);
    }

    @Transactional
    public BuildingResponse update(Long id, BuildingRequest request) {
        UserContext user = userContextProvider.current();
        permissionService.checkUpdate(user, ObjectType.BUILDING, id);
        if (!permissionService.canCreate(user, ObjectType.MILITARY_UNIT, request.unitId(), ObjectType.BUILDING)) {
            throw new AccessDeniedException("Access denied");
        }
        int updated = jdbcTemplate.update("""
                UPDATE buildings
                SET name = :name, unit_id = :unitId, assignable = :assignable
                WHERE building_id = :id
                """, Map.of(
                "id", id,
                "name", request.name(),
                "unitId", request.unitId(),
                "assignable", assignable(request)
        ));
        if (updated == 0) {
            throw new EntityNotFoundException("Building not found: " + id);
        }
        auditService.updated(user, ObjectType.BUILDING, id, "Building updated");
        return findById(id);
    }

    @Transactional
    public void delete(Long id) {
        UserContext user = userContextProvider.current();
        permissionService.checkDelete(user, ObjectType.BUILDING, id);
        int deleted = jdbcTemplate.update("""
                DELETE FROM buildings
                WHERE building_id = :id
                """, Map.of("id", id));
        if (deleted == 0) {
            throw new EntityNotFoundException("Building not found: " + id);
        }
        auditService.deleted(user, ObjectType.BUILDING, id, "Building deleted");
    }

    @Transactional(readOnly = true)
    public List<BuildingResponse> byUnit(Long unitId) {
        UserContext user = userContextProvider.current();
        permissionService.checkRead(user, ObjectType.MILITARY_UNIT, unitId);
        return queryBuildings(new BuildingFilter(null, unitId));
    }

    @Transactional(readOnly = true)
    public BuildingStatisticsResponse statistics() {
        UserContext user = userContextProvider.current();
        List<BuildingResponse> rows = queryBuildings(new BuildingFilter(null, null))
                .stream()
                .filter(row -> permissionService.canRead(user, ObjectType.BUILDING, row.id()))
                .toList();
        long units = rows.stream().map(BuildingResponse::unitId).distinct().count();
        long empty = rows.stream().filter(row -> row.subdivisionsCount() == 0).count();
        long overloaded = rows.stream().filter(row -> row.subdivisionsCount() > 3).count();
        long assigned = rows.size() - empty;
        int readiness = rows.isEmpty() ? 0 : Math.max(0, Math.min(100, (int) (90 - empty * 8 - overloaded * 10)));
        return new BuildingStatisticsResponse(units, (long) rows.size(), assigned, empty, overloaded, readiness);
    }

    @Transactional
    public void assignSubdivision(Long buildingId, Long subdivisionId) {
        UserContext user = userContextProvider.current();
        permissionService.checkUpdate(user, ObjectType.BUILDING, buildingId);
        BuildingResponse building = findById(buildingId);
        Long subdivisionUnitId = jdbcTemplate.query("""
                SELECT unit_id
                FROM subdivisions
                WHERE subdivision_id = :subdivisionId
                """, Map.of("subdivisionId", subdivisionId), rs -> rs.next() ? rs.getLong("unit_id") : null);
        if (subdivisionUnitId == null) {
            throw new EntityNotFoundException("Subdivision not found: " + subdivisionId);
        }
        if (!building.unitId().equals(subdivisionUnitId)) {
            throw new IllegalArgumentException("Подразделение и сооружение должны относиться к одной военной части.");
        }
        if (!Boolean.TRUE.equals(building.assignable())) {
            throw new IllegalArgumentException("Невозможно назначить подразделение: выбранное сооружение не предназначено для размещения подразделений.");
        }
        jdbcTemplate.update("""
                INSERT INTO subdivision_buildings (subdivision_id, building_id)
                VALUES (:subdivisionId, :buildingId)
                ON CONFLICT DO NOTHING
                """, Map.of("subdivisionId", subdivisionId, "buildingId", buildingId));
        auditService.updated(user, ObjectType.BUILDING, buildingId, "Subdivision assigned to building: " + subdivisionId);
    }

    @Transactional
    public void removeSubdivision(Long buildingId, Long subdivisionId) {
        UserContext user = userContextProvider.current();
        permissionService.checkUpdate(user, ObjectType.BUILDING, buildingId);
        jdbcTemplate.update("""
                DELETE FROM subdivision_buildings
                WHERE building_id = :buildingId AND subdivision_id = :subdivisionId
                """, Map.of("subdivisionId", subdivisionId, "buildingId", buildingId));
        auditService.updated(user, ObjectType.BUILDING, buildingId, "Subdivision removed from building: " + subdivisionId);
    }

    private List<BuildingResponse> queryBuildings(BuildingFilter filter) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        if (filter.search() != null && !filter.search().isBlank()) {
            where.append(" AND (lower(b.name) LIKE :search OR lower(mu.name) LIKE :search) ");
            params.put("search", "%" + filter.search().trim().toLowerCase() + "%");
        }
        if (filter.unitId() != null) {
            where.append(" AND b.unit_id = :unitId ");
            params.put("unitId", filter.unitId());
        }

        return jdbcTemplate.query("""
                SELECT b.building_id,
                       b.name,
                       b.unit_id,
                       b.assignable,
                       mu.name AS unit_name,
                       COUNT(sb.subdivision_id) AS subdivisions_count
                FROM buildings b
                JOIN military_units mu ON mu.unit_id = b.unit_id
                LEFT JOIN subdivision_buildings sb ON sb.building_id = b.building_id
                """ + where + """
                GROUP BY b.building_id, b.name, b.unit_id, b.assignable, mu.name
                ORDER BY mu.name, b.name
                """, params, this::mapRow);
    }

    private BuildingResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        long subdivisionsCount = rs.getLong("subdivisions_count");
        String status = subdivisionsCount == 0 ? "WARNING" : subdivisionsCount > 3 ? "OVERLOADED" : "READY";
        return new BuildingResponse(
                rs.getLong("building_id"),
                rs.getString("name"),
                rs.getLong("unit_id"),
                rs.getString("unit_name"),
                rs.getBoolean("assignable"),
                subdivisionsCount,
                status,
                assignedSubdivisions(rs.getLong("building_id"))
        );
    }

    private List<BuildingAssignmentResponse> assignedSubdivisions(Long buildingId) {
        return jdbcTemplate.query("""
                SELECT s.subdivision_id,
                       s.name,
                       s.type,
                       s.unit_id,
                       mu.name AS unit_name
                FROM subdivision_buildings sb
                JOIN subdivisions s ON s.subdivision_id = sb.subdivision_id
                JOIN military_units mu ON mu.unit_id = s.unit_id
                WHERE sb.building_id = :buildingId
                ORDER BY s.type, s.name
                """, Map.of("buildingId", buildingId), (rs, rowNum) -> new BuildingAssignmentResponse(
                rs.getLong("subdivision_id"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getLong("unit_id"),
                rs.getString("unit_name")
        ));
    }

    private boolean assignable(BuildingRequest request) {
        return request.assignable() == null || request.assignable();
    }

    private PageResponse<BuildingResponse> page(List<BuildingResponse> rows, Pageable pageable) {
        int from = Math.min((int) pageable.getOffset(), rows.size());
        int to = Math.min(from + pageable.getPageSize(), rows.size());
        List<BuildingResponse> content = rows.subList(from, to);
        int totalPages = pageable.getPageSize() == 0 ? 0 : (int) Math.ceil((double) rows.size() / pageable.getPageSize());
        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(), rows.size(), totalPages, pageable.getPageNumber() == 0, pageable.getPageNumber() + 1 >= totalPages);
    }
}
