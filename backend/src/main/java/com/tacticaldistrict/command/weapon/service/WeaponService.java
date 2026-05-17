package com.tacticaldistrict.command.weapon.service;

import com.tacticaldistrict.command.audit.AuditService;
import com.tacticaldistrict.command.common.dto.PageResponse;
import com.tacticaldistrict.command.equipment.dto.InventoryQuantityRequest;
import com.tacticaldistrict.command.equipment.dto.InventoryStatisticsResponse;
import com.tacticaldistrict.command.security.access.PermissionService;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import com.tacticaldistrict.command.weapon.dto.UnitWeaponResponse;
import com.tacticaldistrict.command.weapon.dto.WeaponCategoryResponse;
import com.tacticaldistrict.command.weapon.dto.WeaponFilter;
import com.tacticaldistrict.command.weapon.dto.WeaponTypeResponse;
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
public class WeaponService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final UserContextProvider userContextProvider;
    private final PermissionService permissionService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<UnitWeaponResponse> search(WeaponFilter filter, Pageable pageable) {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("weapon:read")) {
            throw new AccessDeniedException("Access denied");
        }
        List<UnitWeaponResponse> rows = queryInventory(filter)
                .stream()
                .filter(row -> permissionService.canRead(user, ObjectType.MILITARY_UNIT, row.unitId()))
                .toList();
        return page(rows, pageable);
    }

    @Transactional(readOnly = true)
    public List<UnitWeaponResponse> byUnit(Long unitId) {
        UserContext user = userContextProvider.current();
        permissionService.checkRead(user, ObjectType.MILITARY_UNIT, unitId);
        return queryInventory(new WeaponFilter(null, unitId, null, null));
    }

    @Transactional
    public UnitWeaponResponse updateUnitWeapon(Long unitId, Long typeId, InventoryQuantityRequest request) {
        UserContext user = userContextProvider.current();
        permissionService.checkUpdate(user, ObjectType.WEAPON, unitId);
        validateType(typeId);
        jdbcTemplate.update("""
                INSERT INTO weapon_in_units (unit_id, type_id, quantity)
                VALUES (:unitId, :typeId, :quantity)
                ON CONFLICT (unit_id, type_id) DO UPDATE SET quantity = EXCLUDED.quantity
                """, Map.of("unitId", unitId, "typeId", typeId, "quantity", request.quantity()));
        auditService.updated(user, ObjectType.WEAPON, unitId, "Weapon quantity updated, typeId=" + typeId);
        return byUnit(unitId).stream()
                .filter(row -> row.typeId().equals(typeId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Weapon row not found"));
    }

    @Transactional
    public void deleteUnitWeapon(Long unitId, Long typeId) {
        UserContext user = userContextProvider.current();
        permissionService.checkDelete(user, ObjectType.WEAPON, unitId);
        jdbcTemplate.update("""
                DELETE FROM weapon_in_units
                WHERE unit_id = :unitId AND type_id = :typeId
                """, Map.of("unitId", unitId, "typeId", typeId));
        auditService.deleted(user, ObjectType.WEAPON, unitId, "Weapon removed, typeId=" + typeId);
    }

    @Transactional(readOnly = true)
    public List<WeaponCategoryResponse> categories() {
        return jdbcTemplate.query("""
                SELECT category_id, name
                FROM weapon_categories
                ORDER BY name
                """, Map.of(), (rs, rowNum) -> new WeaponCategoryResponse(rs.getLong("category_id"), rs.getString("name")));
    }

    @Transactional(readOnly = true)
    public List<WeaponTypeResponse> types() {
        return jdbcTemplate.query("""
                SELECT wt.type_id, wt.name, wc.category_id, wc.name AS category_name
                FROM weapon_types wt
                JOIN weapon_categories wc ON wc.category_id = wt.category_id
                ORDER BY wc.name, wt.name
                """, Map.of(), (rs, rowNum) -> new WeaponTypeResponse(
                rs.getLong("type_id"),
                rs.getString("name"),
                rs.getLong("category_id"),
                rs.getString("category_name")
        ));
    }

    @Transactional(readOnly = true)
    public InventoryStatisticsResponse statistics() {
        UserContext user = userContextProvider.current();
        List<UnitWeaponResponse> rows = queryInventory(new WeaponFilter(null, null, null, null))
                .stream()
                .filter(row -> permissionService.canRead(user, ObjectType.MILITARY_UNIT, row.unitId()))
                .toList();
        long units = rows.stream().map(UnitWeaponResponse::unitId).distinct().count();
        long totalQuantity = rows.stream().mapToLong(UnitWeaponResponse::quantity).sum();
        long warningRows = rows.stream().filter(row -> row.quantity() <= 0).count();
        int readiness = units == 0 || rows.isEmpty() ? 0 : Math.max(0, Math.min(100, (int) (92 - warningRows * 15)));
        return new InventoryStatisticsResponse(units, (long) rows.size(), totalQuantity, warningRows, readiness);
    }

    private List<UnitWeaponResponse> queryInventory(WeaponFilter filter) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        if (filter.search() != null && !filter.search().isBlank()) {
            where.append(" AND (lower(mu.name) LIKE :search OR lower(wt.name) LIKE :search OR lower(wc.name) LIKE :search) ");
            params.put("search", "%" + filter.search().trim().toLowerCase() + "%");
        }
        if (filter.unitId() != null) {
            where.append(" AND mu.unit_id = :unitId ");
            params.put("unitId", filter.unitId());
        }
        if (filter.categoryId() != null) {
            where.append(" AND wc.category_id = :categoryId ");
            params.put("categoryId", filter.categoryId());
        }
        if (filter.typeId() != null) {
            where.append(" AND wt.type_id = :typeId ");
            params.put("typeId", filter.typeId());
        }

        return jdbcTemplate.query("""
                SELECT mu.unit_id, mu.name AS unit_name,
                       wt.type_id, wt.name AS type_name,
                       wc.category_id, wc.name AS category_name,
                       wiu.quantity
                FROM weapon_in_units wiu
                JOIN military_units mu ON mu.unit_id = wiu.unit_id
                JOIN weapon_types wt ON wt.type_id = wiu.type_id
                JOIN weapon_categories wc ON wc.category_id = wt.category_id
                """ + where + " ORDER BY mu.name, wc.name, wt.name", params, this::mapRow);
    }

    private UnitWeaponResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        int quantity = rs.getInt("quantity");
        return new UnitWeaponResponse(
                rs.getLong("unit_id"),
                rs.getString("unit_name"),
                rs.getLong("type_id"),
                rs.getString("type_name"),
                rs.getLong("category_id"),
                rs.getString("category_name"),
                quantity,
                quantity <= 0 ? "WARNING" : quantity < 10 ? "LOW" : "READY"
        );
    }

    private void validateType(Long typeId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM weapon_types WHERE type_id = :typeId)",
                Map.of("typeId", typeId),
                Boolean.class
        );
        if (!Boolean.TRUE.equals(exists)) {
            throw new EntityNotFoundException("Weapon type not found: " + typeId);
        }
    }

    private PageResponse<UnitWeaponResponse> page(List<UnitWeaponResponse> rows, Pageable pageable) {
        int from = Math.min((int) pageable.getOffset(), rows.size());
        int to = Math.min(from + pageable.getPageSize(), rows.size());
        List<UnitWeaponResponse> content = rows.subList(from, to);
        int totalPages = pageable.getPageSize() == 0 ? 0 : (int) Math.ceil((double) rows.size() / pageable.getPageSize());
        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(), rows.size(), totalPages, pageable.getPageNumber() == 0, pageable.getPageNumber() + 1 >= totalPages);
    }
}
