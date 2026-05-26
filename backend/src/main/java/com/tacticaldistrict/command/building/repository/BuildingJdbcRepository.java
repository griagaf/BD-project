package com.tacticaldistrict.command.building.repository;

import com.tacticaldistrict.command.building.dto.BuildingAssignmentResponse;
import com.tacticaldistrict.command.building.dto.BuildingFilter;
import com.tacticaldistrict.command.building.dto.BuildingRequest;
import com.tacticaldistrict.command.building.dto.BuildingResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BuildingJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<BuildingResponse> search(BuildingFilter filter) {
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

    public Optional<BuildingResponse> findById(Long id) {
        return search(new BuildingFilter(null, null))
                .stream()
                .filter(row -> row.id().equals(id))
                .findFirst();
    }

    public Long create(BuildingRequest request, boolean assignable) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO buildings (name, unit_id, assignable)
                VALUES (:name, :unitId, :assignable)
                RETURNING building_id
                """, Map.of(
                "name", request.name(),
                "unitId", request.unitId(),
                "assignable", assignable
        ), Long.class);
    }

    public boolean update(Long id, BuildingRequest request, boolean assignable) {
        int updated = jdbcTemplate.update("""
                UPDATE buildings
                SET name = :name, unit_id = :unitId, assignable = :assignable
                WHERE building_id = :id
                """, Map.of(
                "id", id,
                "name", request.name(),
                "unitId", request.unitId(),
                "assignable", assignable
        ));
        return updated > 0;
    }

    public boolean delete(Long id) {
        int deleted = jdbcTemplate.update("""
                DELETE FROM buildings
                WHERE building_id = :id
                """, Map.of("id", id));
        return deleted > 0;
    }

    public Optional<Long> subdivisionUnitId(Long subdivisionId) {
        Long result = jdbcTemplate.query("""
                SELECT unit_id
                FROM subdivisions
                WHERE subdivision_id = :subdivisionId
                """, Map.of("subdivisionId", subdivisionId), rs -> rs.next() ? rs.getLong("unit_id") : null);
        return Optional.ofNullable(result);
    }

    public Optional<Long> currentBuildingId(Long subdivisionId) {
        Long result = jdbcTemplate.query("""
                SELECT building_id
                FROM subdivision_buildings
                WHERE subdivision_id = :subdivisionId
                """, Map.of("subdivisionId", subdivisionId), rs -> rs.next() ? rs.getLong("building_id") : null);
        return Optional.ofNullable(result);
    }

    public void assignSubdivision(Long buildingId, Long subdivisionId) {
        jdbcTemplate.update("""
                INSERT INTO subdivision_buildings (subdivision_id, building_id)
                VALUES (:subdivisionId, :buildingId)
                ON CONFLICT DO NOTHING
                """, Map.of("subdivisionId", subdivisionId, "buildingId", buildingId));
    }

    public void removeSubdivision(Long buildingId, Long subdivisionId) {
        jdbcTemplate.update("""
                DELETE FROM subdivision_buildings
                WHERE building_id = :buildingId AND subdivision_id = :subdivisionId
                """, Map.of("subdivisionId", subdivisionId, "buildingId", buildingId));
    }

    private BuildingResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        long subdivisionsCount = rs.getLong("subdivisions_count");
        boolean assignable = rs.getBoolean("assignable");
        String status = !assignable ? "DEPLOYMENT_NOT_APPLICABLE" : subdivisionsCount == 0 ? "WARNING" : subdivisionsCount > 3 ? "OVERLOADED" : "READY";
        return new BuildingResponse(
                rs.getLong("building_id"),
                rs.getString("name"),
                rs.getLong("unit_id"),
                rs.getString("unit_name"),
                assignable,
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
}
