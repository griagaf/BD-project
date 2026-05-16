package com.tacticaldistrict.command.equipment.service;

import com.tacticaldistrict.command.audit.AuditService;
import com.tacticaldistrict.command.common.dto.PageResponse;
import com.tacticaldistrict.command.equipment.dto.EquipmentCategoryResponse;
import com.tacticaldistrict.command.equipment.dto.EquipmentFilter;
import com.tacticaldistrict.command.equipment.dto.EquipmentTypeResponse;
import com.tacticaldistrict.command.equipment.dto.InventoryQuantityRequest;
import com.tacticaldistrict.command.equipment.dto.InventoryStatisticsResponse;
import com.tacticaldistrict.command.equipment.dto.UnitEquipmentResponse;
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
public class EquipmentService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final UserContextProvider userContextProvider;
    private final PermissionService permissionService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<UnitEquipmentResponse> search(EquipmentFilter filter, Pageable pageable) {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("equipment:read")) {
            throw new AccessDeniedException("Access denied");
        }
        List<UnitEquipmentResponse> rows = queryInventory(filter)
                .stream()
                .filter(row -> permissionService.canRead(user, ObjectType.MILITARY_UNIT, row.unitId()))
                .toList();
        return page(rows, pageable);
    }

    @Transactional(readOnly = true)
    public List<UnitEquipmentResponse> byUnit(Long unitId) {
        UserContext user = userContextProvider.current();
        permissionService.checkRead(user, ObjectType.MILITARY_UNIT, unitId);
        return queryInventory(new EquipmentFilter(null, unitId, null, null));
    }

    @Transactional
    public UnitEquipmentResponse updateUnitEquipment(Long unitId, Long typeId, InventoryQuantityRequest request) {
        UserContext user = userContextProvider.current();
        permissionService.checkUpdate(user, ObjectType.EQUIPMENT, unitId);
        validateType(typeId);
        jdbcTemplate.update("""
                INSERT INTO equipment_in_units (unit_id, type_id, quantity)
                VALUES (:unitId, :typeId, :quantity)
                ON CONFLICT (unit_id, type_id) DO UPDATE SET quantity = EXCLUDED.quantity
                """, Map.of("unitId", unitId, "typeId", typeId, "quantity", request.quantity()));
        auditService.updated(user, ObjectType.EQUIPMENT, unitId, "Equipment quantity updated, typeId=" + typeId);
        return byUnit(unitId).stream()
                .filter(row -> row.typeId().equals(typeId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Equipment row not found"));
    }

    @Transactional
    public void deleteUnitEquipment(Long unitId, Long typeId) {
        UserContext user = userContextProvider.current();
        permissionService.checkDelete(user, ObjectType.EQUIPMENT, unitId);
        jdbcTemplate.update("""
                DELETE FROM equipment_in_units
                WHERE unit_id = :unitId AND type_id = :typeId
                """, Map.of("unitId", unitId, "typeId", typeId));
        auditService.deleted(user, ObjectType.EQUIPMENT, unitId, "Equipment removed, typeId=" + typeId);
    }

    @Transactional(readOnly = true)
    public List<EquipmentCategoryResponse> categories() {
        return jdbcTemplate.query("""
                SELECT category_id, name
                FROM equipment_categories
                ORDER BY name
                """, Map.of(), (rs, rowNum) -> new EquipmentCategoryResponse(rs.getLong("category_id"), rs.getString("name")));
    }

    @Transactional(readOnly = true)
    public List<EquipmentTypeResponse> types() {
        return jdbcTemplate.query("""
                SELECT et.type_id, et.name, ec.category_id, ec.name AS category_name
                FROM equipment_types et
                JOIN equipment_categories ec ON ec.category_id = et.category_id
                ORDER BY ec.name, et.name
                """, Map.of(), (rs, rowNum) -> new EquipmentTypeResponse(
                rs.getLong("type_id"),
                rs.getString("name"),
                rs.getLong("category_id"),
                rs.getString("category_name")
        ));
    }

    @Transactional(readOnly = true)
    public InventoryStatisticsResponse statistics() {
        UserContext user = userContextProvider.current();
        List<UnitEquipmentResponse> rows = queryInventory(new EquipmentFilter(null, null, null, null))
                .stream()
                .filter(row -> permissionService.canRead(user, ObjectType.MILITARY_UNIT, row.unitId()))
                .toList();
        long units = rows.stream().map(UnitEquipmentResponse::unitId).distinct().count();
        long totalQuantity = rows.stream().mapToLong(UnitEquipmentResponse::quantity).sum();
        long warningRows = rows.stream().filter(row -> row.quantity() <= 0).count();
        int readiness = readiness(units, rows.size(), warningRows);
        return new InventoryStatisticsResponse(units, (long) rows.size(), totalQuantity, warningRows, readiness);
    }

    private List<UnitEquipmentResponse> queryInventory(EquipmentFilter filter) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        if (filter.search() != null && !filter.search().isBlank()) {
            where.append(" AND (lower(mu.name) LIKE :search OR lower(et.name) LIKE :search OR lower(ec.name) LIKE :search) ");
            params.put("search", "%" + filter.search().trim().toLowerCase() + "%");
        }
        if (filter.unitId() != null) {
            where.append(" AND mu.unit_id = :unitId ");
            params.put("unitId", filter.unitId());
        }
        if (filter.categoryId() != null) {
            where.append(" AND ec.category_id = :categoryId ");
            params.put("categoryId", filter.categoryId());
        }
        if (filter.typeId() != null) {
            where.append(" AND et.type_id = :typeId ");
            params.put("typeId", filter.typeId());
        }

        return jdbcTemplate.query("""
                SELECT mu.unit_id, mu.name AS unit_name,
                       et.type_id, et.name AS type_name,
                       ec.category_id, ec.name AS category_name,
                       eiu.quantity
                FROM equipment_in_units eiu
                JOIN military_units mu ON mu.unit_id = eiu.unit_id
                JOIN equipment_types et ON et.type_id = eiu.type_id
                JOIN equipment_categories ec ON ec.category_id = et.category_id
                """ + where + " ORDER BY mu.name, ec.name, et.name", params, this::mapRow);
    }

    private UnitEquipmentResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        int quantity = rs.getInt("quantity");
        return new UnitEquipmentResponse(
                rs.getLong("unit_id"),
                rs.getString("unit_name"),
                rs.getLong("type_id"),
                rs.getString("type_name"),
                rs.getLong("category_id"),
                rs.getString("category_name"),
                quantity,
                quantity <= 0 ? "WARNING" : quantity < 5 ? "LOW" : "READY"
        );
    }

    private void validateType(Long typeId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM equipment_types WHERE type_id = :typeId)",
                Map.of("typeId", typeId),
                Boolean.class
        );
        if (!Boolean.TRUE.equals(exists)) {
            throw new EntityNotFoundException("Equipment type not found: " + typeId);
        }
    }

    private PageResponse<UnitEquipmentResponse> page(List<UnitEquipmentResponse> rows, Pageable pageable) {
        int from = Math.min((int) pageable.getOffset(), rows.size());
        int to = Math.min(from + pageable.getPageSize(), rows.size());
        List<UnitEquipmentResponse> content = rows.subList(from, to);
        int totalPages = pageable.getPageSize() == 0 ? 0 : (int) Math.ceil((double) rows.size() / pageable.getPageSize());
        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(), rows.size(), totalPages, pageable.getPageNumber() == 0, pageable.getPageNumber() + 1 >= totalPages);
    }

    private int readiness(long units, long rows, long warningRows) {
        if (units == 0 || rows == 0) {
            return 0;
        }
        return Math.max(0, Math.min(100, (int) (90 - warningRows * 15)));
    }
}
