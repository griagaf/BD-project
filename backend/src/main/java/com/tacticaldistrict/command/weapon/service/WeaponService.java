package com.tacticaldistrict.command.weapon.service;

import com.tacticaldistrict.command.audit.AuditService;
import com.tacticaldistrict.command.common.dto.AttributeValueResponse;
import com.tacticaldistrict.command.common.dto.PageResponse;
import com.tacticaldistrict.command.equipment.dto.InventoryQuantityRequest;
import com.tacticaldistrict.command.equipment.dto.InventoryStatisticsResponse;
import com.tacticaldistrict.command.equipment.dto.InventoryCategoryRequest;
import com.tacticaldistrict.command.security.access.PermissionService;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import com.tacticaldistrict.command.weapon.dto.UnitWeaponResponse;
import com.tacticaldistrict.command.weapon.dto.WeaponCategoryResponse;
import com.tacticaldistrict.command.weapon.dto.WeaponFilter;
import com.tacticaldistrict.command.weapon.dto.WeaponTypePassportResponse;
import com.tacticaldistrict.command.weapon.dto.WeaponTypeRequest;
import com.tacticaldistrict.command.weapon.dto.WeaponTypeResponse;
import jakarta.persistence.EntityNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
                WHERE archived = FALSE
                ORDER BY name
                """, Map.of(), (rs, rowNum) -> new WeaponCategoryResponse(rs.getLong("category_id"), rs.getString("name")));
    }

    @Transactional(readOnly = true)
    public List<WeaponTypeResponse> types() {
        return jdbcTemplate.query("""
                SELECT wt.type_id, wt.name, wc.category_id, wc.name AS category_name
                FROM weapon_types wt
                JOIN weapon_categories wc ON wc.category_id = wt.category_id
                WHERE wt.archived = FALSE AND wc.archived = FALSE
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

    @Transactional
    public WeaponCategoryResponse createCategory(InventoryCategoryRequest request) {
        UserContext user = userContextProvider.current();
        checkDictionaryManagement(user);
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO weapon_categories (name)
                VALUES (:name)
                RETURNING category_id
                """, Map.of("name", request.name().trim()), Long.class);
        auditService.created(user, ObjectType.WEAPON, id, "Weapon category created");
        return new WeaponCategoryResponse(id, request.name().trim());
    }

    @Transactional
    public WeaponCategoryResponse updateCategory(Long id, InventoryCategoryRequest request) {
        UserContext user = userContextProvider.current();
        checkDictionaryManagement(user);
        int updated = jdbcTemplate.update("""
                UPDATE weapon_categories
                SET name = :name
                WHERE category_id = :id AND archived = FALSE
                """, Map.of("id", id, "name", request.name().trim()));
        if (updated == 0) {
            throw new EntityNotFoundException("Weapon category not found: " + id);
        }
        auditService.updated(user, ObjectType.WEAPON, id, "Weapon category updated");
        return new WeaponCategoryResponse(id, request.name().trim());
    }

    @Transactional
    public void archiveCategory(Long id) {
        UserContext user = userContextProvider.current();
        checkDictionaryManagement(user);
        jdbcTemplate.update("UPDATE weapon_categories SET archived = TRUE WHERE category_id = :id", Map.of("id", id));
        auditService.deleted(user, ObjectType.WEAPON, id, "Weapon category archived");
    }

    @Transactional
    public WeaponTypeResponse createType(WeaponTypeRequest request) {
        UserContext user = userContextProvider.current();
        checkDictionaryManagement(user);
        validateCategory(request.categoryId());
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO weapon_types (
                    name, category_id, purpose, caliber, effective_range_m,
                    adoption_year, manufacturer, description
                )
                VALUES (
                    :name, :categoryId, :purpose, :caliber, :effectiveRangeM,
                    :adoptionYear, :manufacturer, :description
                )
                RETURNING type_id
                """, typeParams(request), Long.class);
        syncTypeAttributes(id, request);
        auditService.created(user, ObjectType.WEAPON, id, "Weapon type created");
        return typeById(id);
    }

    @Transactional
    public WeaponTypeResponse updateType(Long id, WeaponTypeRequest request) {
        UserContext user = userContextProvider.current();
        checkDictionaryManagement(user);
        validateCategory(request.categoryId());
        Map<String, Object> params = typeParams(request);
        params.put("id", id);
        int updated = jdbcTemplate.update("""
                UPDATE weapon_types
                SET name = :name,
                    category_id = :categoryId,
                    purpose = :purpose,
                    caliber = :caliber,
                    effective_range_m = :effectiveRangeM,
                    adoption_year = :adoptionYear,
                    manufacturer = :manufacturer,
                    description = :description
                WHERE type_id = :id AND archived = FALSE
                """, params);
        if (updated == 0) {
            throw new EntityNotFoundException("Weapon type not found: " + id);
        }
        syncTypeAttributes(id, request);
        auditService.updated(user, ObjectType.WEAPON, id, "Weapon type updated");
        return typeById(id);
    }

    @Transactional
    public void archiveType(Long id) {
        UserContext user = userContextProvider.current();
        checkDictionaryManagement(user);
        jdbcTemplate.update("UPDATE weapon_types SET archived = TRUE WHERE type_id = :id", Map.of("id", id));
        auditService.deleted(user, ObjectType.WEAPON, id, "Weapon type archived");
    }

    @Transactional(readOnly = true)
    public WeaponTypePassportResponse typePassport(Long id) {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("weapon:read")) {
            throw new AccessDeniedException("Access denied");
        }
        WeaponTypePassportResponse base = jdbcTemplate.queryForObject("""
                SELECT wt.type_id, wt.name, wc.category_id, wc.name AS category_name,
                       wt.purpose, wt.caliber, wt.effective_range_m, wt.adoption_year,
                       wt.manufacturer, wt.description,
                       COALESCE(SUM(wiu.quantity), 0) AS total_quantity,
                       COUNT(DISTINCT wiu.unit_id) AS units_count
                FROM weapon_types wt
                JOIN weapon_categories wc ON wc.category_id = wt.category_id
                LEFT JOIN weapon_in_units wiu ON wiu.type_id = wt.type_id
                WHERE wt.type_id = :id AND wt.archived = FALSE
                GROUP BY wt.type_id, wt.name, wc.category_id, wc.name,
                         wt.purpose, wt.caliber, wt.effective_range_m, wt.adoption_year,
                         wt.manufacturer, wt.description
                """, Map.of("id", id), this::mapTypePassport);
        if (base == null) {
            throw new EntityNotFoundException("Weapon type not found: " + id);
        }
        List<UnitWeaponResponse> distribution = queryInventory(new WeaponFilter(null, null, null, id))
                .stream()
                .filter(row -> permissionService.canRead(user, ObjectType.MILITARY_UNIT, row.unitId()))
                .toList();
        long totalQuantity = distribution.stream().mapToLong(UnitWeaponResponse::quantity).sum();
        long unitsCount = distribution.stream().map(UnitWeaponResponse::unitId).distinct().count();
        List<AttributeValueResponse> attributes = typeAttributes(id);
        return new WeaponTypePassportResponse(
                base.id(), base.name(), base.categoryId(), base.categoryName(), base.purpose(),
                base.caliber(), base.effectiveRangeM(), base.adoptionYear(), base.manufacturer(),
                base.description(), totalQuantity, unitsCount, attributes, distribution
        );
    }

    private List<AttributeValueResponse> typeAttributes(Long typeId) {
        return jdbcTemplate.query("""
                SELECT wat.attribute_id,
                       wat.name,
                       wat.data_type,
                       COALESCE(
                           wtav.value_text,
                           trim(to_char(wtav.value_number, 'FM999999990.99')),
                           to_char(wtav.value_date, 'YYYY-MM-DD'),
                           CASE WHEN wtav.value_boolean IS NULL THEN NULL ELSE wtav.value_boolean::TEXT END
                       ) AS display_value
                FROM weapon_type_attribute_values wtav
                JOIN weapon_attribute_types wat ON wat.attribute_id = wtav.attribute_id
                WHERE wtav.type_id = :typeId
                  AND COALESCE(
                      wtav.value_text,
                      wtav.value_number::TEXT,
                      wtav.value_date::TEXT,
                      wtav.value_boolean::TEXT
                  ) IS NOT NULL
                ORDER BY wat.attribute_id
                """, Map.of("typeId", typeId), (rs, rowNum) -> new AttributeValueResponse(
                rs.getLong("attribute_id"),
                rs.getString("name"),
                rs.getString("data_type"),
                rs.getString("display_value")
        ));
    }

    private void syncTypeAttributes(Long typeId, WeaponTypeRequest request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("назначение", blankToNull(request.purpose()));
        values.put("калибр", blankToNull(request.caliber()));
        values.put("дальность, м", request.effectiveRangeM());
        values.put("год принятия", request.adoptionYear());
        values.put("производитель", blankToNull(request.manufacturer()));
        values.put("описание", blankToNull(request.description()));
        syncAttributeValues(typeId, values);
    }

    private void syncAttributeValues(Long typeId, Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Long attributeId = jdbcTemplate.queryForObject(
                    "SELECT attribute_id FROM weapon_attribute_types WHERE name = :name",
                    Map.of("name", entry.getKey()),
                    Long.class
            );
            Object value = entry.getValue();
            if (value == null) {
                jdbcTemplate.update("""
                        DELETE FROM weapon_type_attribute_values
                        WHERE type_id = :typeId AND attribute_id = :attributeId
                        """, Map.of("typeId", typeId, "attributeId", attributeId));
                continue;
            }
            Map<String, Object> params = new HashMap<>();
            params.put("typeId", typeId);
            params.put("attributeId", attributeId);
            params.put("valueText", value instanceof Number ? null : value.toString());
            params.put("valueNumber", value instanceof Number ? value : null);
            jdbcTemplate.update("""
                    INSERT INTO weapon_type_attribute_values (type_id, attribute_id, value_text, value_number)
                    VALUES (:typeId, :attributeId, :valueText, :valueNumber)
                    ON CONFLICT (type_id, attribute_id) DO UPDATE SET
                        value_text = EXCLUDED.value_text,
                        value_number = EXCLUDED.value_number,
                        value_date = NULL,
                        value_boolean = NULL
                    """, params);
        }
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
                """ + where + " AND wt.archived = FALSE AND wc.archived = FALSE ORDER BY mu.name, wc.name, wt.name", params, this::mapRow);
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
                "SELECT EXISTS (SELECT 1 FROM weapon_types WHERE type_id = :typeId AND archived = FALSE)",
                Map.of("typeId", typeId),
                Boolean.class
        );
        if (!Boolean.TRUE.equals(exists)) {
            throw new EntityNotFoundException("Weapon type not found: " + typeId);
        }
    }

    private void validateCategory(Long categoryId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM weapon_categories WHERE category_id = :categoryId AND archived = FALSE)",
                Map.of("categoryId", categoryId),
                Boolean.class
        );
        if (!Boolean.TRUE.equals(exists)) {
            throw new EntityNotFoundException("Weapon category not found: " + categoryId);
        }
    }

    private WeaponTypeResponse typeById(Long id) {
        WeaponTypeResponse response = jdbcTemplate.queryForObject("""
                SELECT wt.type_id, wt.name, wc.category_id, wc.name AS category_name
                FROM weapon_types wt
                JOIN weapon_categories wc ON wc.category_id = wt.category_id
                WHERE wt.type_id = :id AND wt.archived = FALSE
                """, Map.of("id", id), (rs, rowNum) -> new WeaponTypeResponse(
                rs.getLong("type_id"),
                rs.getString("name"),
                rs.getLong("category_id"),
                rs.getString("category_name")
        ));
        if (response == null) {
            throw new EntityNotFoundException("Weapon type not found: " + id);
        }
        return response;
    }

    private WeaponTypePassportResponse mapTypePassport(ResultSet rs, int rowNum) throws SQLException {
        return new WeaponTypePassportResponse(
                rs.getLong("type_id"),
                rs.getString("name"),
                rs.getLong("category_id"),
                rs.getString("category_name"),
                rs.getString("purpose"),
                rs.getString("caliber"),
                (Integer) rs.getObject("effective_range_m"),
                (Integer) rs.getObject("adoption_year"),
                rs.getString("manufacturer"),
                rs.getString("description"),
                rs.getLong("total_quantity"),
                rs.getLong("units_count"),
                List.of(),
                List.of()
        );
    }

    private Map<String, Object> typeParams(WeaponTypeRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", request.name().trim());
        params.put("categoryId", request.categoryId());
        params.put("purpose", blankToNull(request.purpose()));
        params.put("caliber", blankToNull(request.caliber()));
        params.put("effectiveRangeM", request.effectiveRangeM());
        params.put("adoptionYear", request.adoptionYear());
        params.put("manufacturer", blankToNull(request.manufacturer()));
        params.put("description", blankToNull(request.description()));
        return params;
    }

    private void checkDictionaryManagement(UserContext user) {
        if (!user.hasRole(RoleCode.ADMIN_DISTRICT)) {
            throw new AccessDeniedException("Only district administrator can manage weapon dictionaries");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PageResponse<UnitWeaponResponse> page(List<UnitWeaponResponse> rows, Pageable pageable) {
        int from = Math.min((int) pageable.getOffset(), rows.size());
        int to = Math.min(from + pageable.getPageSize(), rows.size());
        List<UnitWeaponResponse> content = rows.subList(from, to);
        int totalPages = pageable.getPageSize() == 0 ? 0 : (int) Math.ceil((double) rows.size() / pageable.getPageSize());
        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(), rows.size(), totalPages, pageable.getPageNumber() == 0, pageable.getPageNumber() + 1 >= totalPages);
    }
}
