package com.tacticaldistrict.command.dashboard.repository;

import com.tacticaldistrict.command.dashboard.application.port.DashboardRepositoryPort;
import com.tacticaldistrict.command.dashboard.dto.AuditEventDto;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DashboardQueryRepository implements DashboardRepositoryPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<Long> formationIds() {
        return ids("SELECT formation_id FROM military_formations");
    }

    @Override
    public List<Long> unitIds() {
        return ids("SELECT unit_id FROM military_units");
    }

    @Override
    public List<Long> subdivisionIds() {
        return ids("SELECT subdivision_id FROM subdivisions");
    }

    @Override
    public List<Long> personnelIds() {
        return ids("SELECT personnel_id FROM personnel");
    }

    @Override
    public List<Long> buildingIds() {
        return ids("SELECT building_id FROM buildings");
    }

    @Override
    public List<InventoryQuantityRow> equipmentQuantities() {
        return inventoryQuantities("equipment_in_units");
    }

    @Override
    public List<InventoryQuantityRow> weaponQuantities() {
        return inventoryQuantities("weapon_in_units");
    }

    @Override
    public List<AuditEventDto> latestEvents(int limit) {
        return jdbcTemplate.query("""
                SELECT audit_event_id, actor_username, action, object_type, object_id, details, created_at
                FROM audit_events
                ORDER BY created_at DESC, audit_event_id DESC
                LIMIT :limit
                """, Map.of("limit", limit), (rs, rowNum) -> {
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
        });
    }

    private List<InventoryQuantityRow> inventoryQuantities(String table) {
        return jdbcTemplate.query("""
                SELECT unit_id, quantity
                FROM %s
                """.formatted(table), Map.of(), (rs, rowNum) -> new InventoryQuantityRow(
                rs.getLong("unit_id"),
                rs.getLong("quantity")
        ));
    }

    private List<Long> ids(String sql) {
        return jdbcTemplate.query(sql, Map.of(), (rs, rowNum) -> rs.getLong(1));
    }
}
