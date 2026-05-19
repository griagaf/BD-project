package com.tacticaldistrict.command.equipment.service;

import com.tacticaldistrict.command.audit.AuditService;
import com.tacticaldistrict.command.common.dto.AttributeValueResponse;
import com.tacticaldistrict.command.common.dto.PageResponse;
import com.tacticaldistrict.command.equipment.dto.EquipmentCategoryResponse;
import com.tacticaldistrict.command.equipment.dto.EquipmentFilter;
import com.tacticaldistrict.command.equipment.dto.EquipmentTypePassportResponse;
import com.tacticaldistrict.command.equipment.dto.EquipmentTypeRequest;
import com.tacticaldistrict.command.equipment.dto.InventoryCategoryRequest;
import com.tacticaldistrict.command.equipment.dto.EquipmentTypeResponse;
import com.tacticaldistrict.command.equipment.dto.InventoryQuantityRequest;
import com.tacticaldistrict.command.equipment.dto.InventoryStatisticsResponse;
import com.tacticaldistrict.command.equipment.dto.UnitEquipmentResponse;
import com.tacticaldistrict.command.security.access.PermissionService;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
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
                WHERE archived = FALSE
                ORDER BY name
                """, Map.of(), (rs, rowNum) -> new EquipmentCategoryResponse(rs.getLong("category_id"), rs.getString("name")));
    }

    @Transactional(readOnly = true)
    public List<EquipmentTypeResponse> types() {
        return jdbcTemplate.query("""
                SELECT et.type_id, et.name, ec.category_id, ec.name AS category_name
                FROM equipment_types et
                JOIN equipment_categories ec ON ec.category_id = et.category_id
                WHERE et.archived = FALSE AND ec.archived = FALSE
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

    @Transactional
    public EquipmentCategoryResponse createCategory(InventoryCategoryRequest request) {
        UserContext user = userContextProvider.current();
        checkDictionaryManagement(user);
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO equipment_categories (name)
                VALUES (:name)
                RETURNING category_id
                """, Map.of("name", request.name().trim()), Long.class);
        auditService.created(user, ObjectType.EQUIPMENT, id, "Equipment category created");
        return new EquipmentCategoryResponse(id, request.name().trim());
    }

    @Transactional
    public EquipmentCategoryResponse updateCategory(Long id, InventoryCategoryRequest request) {
        UserContext user = userContextProvider.current();
        checkDictionaryManagement(user);
        int updated = jdbcTemplate.update("""
                UPDATE equipment_categories
                SET name = :name
                WHERE category_id = :id AND archived = FALSE
                """, Map.of("id", id, "name", request.name().trim()));
        if (updated == 0) {
            throw new EntityNotFoundException("Equipment category not found: " + id);
        }
        auditService.updated(user, ObjectType.EQUIPMENT, id, "Equipment category updated");
        return new EquipmentCategoryResponse(id, request.name().trim());
    }

    @Transactional
    public void archiveCategory(Long id) {
        UserContext user = userContextProvider.current();
        checkDictionaryManagement(user);
        jdbcTemplate.update("UPDATE equipment_categories SET archived = TRUE WHERE category_id = :id", Map.of("id", id));
        auditService.deleted(user, ObjectType.EQUIPMENT, id, "Equipment category archived");
    }

    @Transactional
    public EquipmentTypeResponse createType(EquipmentTypeRequest request) {
        UserContext user = userContextProvider.current();
        checkDictionaryManagement(user);
        validateCategory(request.categoryId());
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO equipment_types (
                    name, category_id, purpose, crew_size, weight_tons, max_speed_kmh,
                    operational_range_km, adoption_year, manufacturer, description
                )
                VALUES (
                    :name, :categoryId, :purpose, :crewSize, :weightTons, :maxSpeedKmh,
                    :operationalRangeKm, :adoptionYear, :manufacturer, :description
                )
                RETURNING type_id
                """, typeParams(request), Long.class);
        syncTypeAttributes(id, request);
        auditService.created(user, ObjectType.EQUIPMENT, id, "Equipment type created");
        return typeById(id);
    }

    @Transactional
    public EquipmentTypeResponse updateType(Long id, EquipmentTypeRequest request) {
        UserContext user = userContextProvider.current();
        checkDictionaryManagement(user);
        validateCategory(request.categoryId());
        Map<String, Object> params = typeParams(request);
        params.put("id", id);
        int updated = jdbcTemplate.update("""
                UPDATE equipment_types
                SET name = :name,
                    category_id = :categoryId,
                    purpose = :purpose,
                    crew_size = :crewSize,
                    weight_tons = :weightTons,
                    max_speed_kmh = :maxSpeedKmh,
                    operational_range_km = :operationalRangeKm,
                    adoption_year = :adoptionYear,
                    manufacturer = :manufacturer,
                    description = :description
                WHERE type_id = :id AND archived = FALSE
                """, params);
        if (updated == 0) {
            throw new EntityNotFoundException("Equipment type not found: " + id);
        }
        syncTypeAttributes(id, request);
        auditService.updated(user, ObjectType.EQUIPMENT, id, "Equipment type updated");
        return typeById(id);
    }

    @Transactional
    public void archiveType(Long id) {
        UserContext user = userContextProvider.current();
        checkDictionaryManagement(user);
        jdbcTemplate.update("UPDATE equipment_types SET archived = TRUE WHERE type_id = :id", Map.of("id", id));
        auditService.deleted(user, ObjectType.EQUIPMENT, id, "Equipment type archived");
    }

    @Transactional(readOnly = true)
    public EquipmentTypePassportResponse typePassport(Long id) {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("equipment:read")) {
            throw new AccessDeniedException("Access denied");
        }
        EquipmentTypePassportResponse base = jdbcTemplate.queryForObject("""
                SELECT et.type_id, et.name, ec.category_id, ec.name AS category_name,
                       et.purpose, et.crew_size, et.weight_tons, et.max_speed_kmh,
                       et.operational_range_km, et.adoption_year, et.manufacturer, et.description,
                       COALESCE(SUM(eiu.quantity), 0) AS total_quantity,
                       COUNT(DISTINCT eiu.unit_id) AS units_count
                FROM equipment_types et
                JOIN equipment_categories ec ON ec.category_id = et.category_id
                LEFT JOIN equipment_in_units eiu ON eiu.type_id = et.type_id
                WHERE et.type_id = :id AND et.archived = FALSE
                GROUP BY et.type_id, et.name, ec.category_id, ec.name,
                         et.purpose, et.crew_size, et.weight_tons, et.max_speed_kmh,
                         et.operational_range_km, et.adoption_year, et.manufacturer, et.description
                """, Map.of("id", id), this::mapTypePassport);
        if (base == null) {
            throw new EntityNotFoundException("Equipment type not found: " + id);
        }
        List<UnitEquipmentResponse> distribution = queryInventory(new EquipmentFilter(null, null, null, id))
                .stream()
                .filter(row -> permissionService.canRead(user, ObjectType.MILITARY_UNIT, row.unitId()))
                .toList();
        long totalQuantity = distribution.stream().mapToLong(UnitEquipmentResponse::quantity).sum();
        long unitsCount = distribution.stream().map(UnitEquipmentResponse::unitId).distinct().count();
        List<AttributeValueResponse> attributes = typeAttributes(id);
        return new EquipmentTypePassportResponse(
                base.id(), base.name(), base.categoryId(), base.categoryName(), base.purpose(),
                base.crewSize(), base.weightTons(), base.maxSpeedKmh(), base.operationalRangeKm(),
                base.adoptionYear(), base.manufacturer(), base.description(),
                totalQuantity, unitsCount, attributes, distribution
        );
    }

    private List<AttributeValueResponse> typeAttributes(Long typeId) {
        return jdbcTemplate.query("""
                SELECT eat.attribute_id,
                       eat.name,
                       eat.data_type,
                       COALESCE(
                           etav.value_text,
                           trim(to_char(etav.value_number, 'FM999999990.99')),
                           to_char(etav.value_date, 'YYYY-MM-DD'),
                           CASE WHEN etav.value_boolean IS NULL THEN NULL ELSE etav.value_boolean::TEXT END
                       ) AS display_value
                FROM equipment_type_attribute_values etav
                JOIN equipment_attribute_types eat ON eat.attribute_id = etav.attribute_id
                WHERE etav.type_id = :typeId
                  AND COALESCE(
                      etav.value_text,
                      etav.value_number::TEXT,
                      etav.value_date::TEXT,
                      etav.value_boolean::TEXT
                  ) IS NOT NULL
                ORDER BY eat.attribute_id
                """, Map.of("typeId", typeId), (rs, rowNum) -> new AttributeValueResponse(
                rs.getLong("attribute_id"),
                rs.getString("name"),
                rs.getString("data_type"),
                rs.getString("display_value")
        ));
    }

    private void syncTypeAttributes(Long typeId, EquipmentTypeRequest request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("назначение", blankToNull(request.purpose()));
        values.put("экипаж", request.crewSize());
        values.put("масса, т", request.weightTons());
        values.put("скорость, км/ч", request.maxSpeedKmh());
        values.put("запас хода, км", request.operationalRangeKm());
        values.put("год принятия", request.adoptionYear());
        values.put("производитель", blankToNull(request.manufacturer()));
        values.put("описание", blankToNull(request.description()));
        syncAttributeValues(typeId, values);
    }

    private void syncAttributeValues(Long typeId, Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Long attributeId = jdbcTemplate.queryForObject(
                    "SELECT attribute_id FROM equipment_attribute_types WHERE name = :name",
                    Map.of("name", entry.getKey()),
                    Long.class
            );
            Object value = entry.getValue();
            if (value == null) {
                jdbcTemplate.update("""
                        DELETE FROM equipment_type_attribute_values
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
                    INSERT INTO equipment_type_attribute_values (type_id, attribute_id, value_text, value_number)
                    VALUES (:typeId, :attributeId, :valueText, :valueNumber)
                    ON CONFLICT (type_id, attribute_id) DO UPDATE SET
                        value_text = EXCLUDED.value_text,
                        value_number = EXCLUDED.value_number,
                        value_date = NULL,
                        value_boolean = NULL
                    """, params);
        }
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
                """ + where + " AND et.archived = FALSE AND ec.archived = FALSE ORDER BY mu.name, ec.name, et.name", params, this::mapRow);
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
                "SELECT EXISTS (SELECT 1 FROM equipment_types WHERE type_id = :typeId AND archived = FALSE)",
                Map.of("typeId", typeId),
                Boolean.class
        );
        if (!Boolean.TRUE.equals(exists)) {
            throw new EntityNotFoundException("Equipment type not found: " + typeId);
        }
    }

    private void validateCategory(Long categoryId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM equipment_categories WHERE category_id = :categoryId AND archived = FALSE)",
                Map.of("categoryId", categoryId),
                Boolean.class
        );
        if (!Boolean.TRUE.equals(exists)) {
            throw new EntityNotFoundException("Equipment category not found: " + categoryId);
        }
    }

    private EquipmentTypeResponse typeById(Long id) {
        EquipmentTypeResponse response = jdbcTemplate.queryForObject("""
                SELECT et.type_id, et.name, ec.category_id, ec.name AS category_name
                FROM equipment_types et
                JOIN equipment_categories ec ON ec.category_id = et.category_id
                WHERE et.type_id = :id AND et.archived = FALSE
                """, Map.of("id", id), (rs, rowNum) -> new EquipmentTypeResponse(
                rs.getLong("type_id"),
                rs.getString("name"),
                rs.getLong("category_id"),
                rs.getString("category_name")
        ));
        if (response == null) {
            throw new EntityNotFoundException("Equipment type not found: " + id);
        }
        return response;
    }

    private EquipmentTypePassportResponse mapTypePassport(ResultSet rs, int rowNum) throws SQLException {
        BigDecimal weight = rs.getBigDecimal("weight_tons");
        return new EquipmentTypePassportResponse(
                rs.getLong("type_id"),
                rs.getString("name"),
                rs.getLong("category_id"),
                rs.getString("category_name"),
                rs.getString("purpose"),
                (Integer) rs.getObject("crew_size"),
                weight,
                (Integer) rs.getObject("max_speed_kmh"),
                (Integer) rs.getObject("operational_range_km"),
                (Integer) rs.getObject("adoption_year"),
                rs.getString("manufacturer"),
                rs.getString("description"),
                rs.getLong("total_quantity"),
                rs.getLong("units_count"),
                List.of(),
                List.of()
        );
    }

    private Map<String, Object> typeParams(EquipmentTypeRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", request.name().trim());
        params.put("categoryId", request.categoryId());
        params.put("purpose", blankToNull(request.purpose()));
        params.put("crewSize", request.crewSize());
        params.put("weightTons", request.weightTons());
        params.put("maxSpeedKmh", request.maxSpeedKmh());
        params.put("operationalRangeKm", request.operationalRangeKm());
        params.put("adoptionYear", request.adoptionYear());
        params.put("manufacturer", blankToNull(request.manufacturer()));
        params.put("description", blankToNull(request.description()));
        return params;
    }

    private void checkDictionaryManagement(UserContext user) {
        if (!user.hasRole(RoleCode.ADMIN_DISTRICT)) {
            throw new AccessDeniedException("Only district administrator can manage equipment dictionaries");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
