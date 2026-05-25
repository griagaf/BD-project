package com.tacticaldistrict.command.common.attribute;

import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DynamicAttributeService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final UserContextProvider userContextProvider;

    @Transactional(readOnly = true)
    public List<DynamicAttributeMetadataResponse> types(String resource) {
        checkRead(resource);
        AttributeTables tables = tables(resource);
        return jdbcTemplate.query("""
                SELECT attribute_id, name, data_type, FALSE AS is_required
                FROM %s
                ORDER BY name
                """.formatted(tables.attributeTypes()), Map.of(), (rs, rowNum) -> new DynamicAttributeMetadataResponse(
                rs.getLong("attribute_id"),
                rs.getString("name"),
                rs.getString("data_type"),
                rs.getBoolean("is_required")
        ));
    }

    @Transactional(readOnly = true)
    public List<DynamicAttributeMetadataResponse> categorySchema(String resource, Long categoryId) {
        checkRead(resource);
        AttributeTables tables = tables(resource);
        return jdbcTemplate.query("""
                SELECT at.attribute_id, at.name, at.data_type, ca.is_required
                FROM %s ca
                JOIN %s at ON at.attribute_id = ca.attribute_id
                WHERE ca.category_id = :categoryId
                ORDER BY at.attribute_id
                """.formatted(tables.categoryAttributes(), tables.attributeTypes()), Map.of("categoryId", categoryId), (rs, rowNum) -> new DynamicAttributeMetadataResponse(
                rs.getLong("attribute_id"),
                rs.getString("name"),
                rs.getString("data_type"),
                rs.getBoolean("is_required")
        ));
    }

    @Transactional(readOnly = true)
    public List<DynamicAttributeMetadataResponse> rankSchema(Long rankId) {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("personnel:read")) {
            throw new AccessDeniedException("Access denied");
        }
        return jdbcTemplate.query("""
                SELECT rat.attribute_id, rat.name, rat.data_type, rta.is_required
                FROM rank_type_attributes rta
                JOIN rank_attribute_types rat ON rat.attribute_id = rta.attribute_id
                WHERE rta.rank_id = :rankId
                ORDER BY rat.attribute_id
                """, Map.of("rankId", rankId), (rs, rowNum) -> new DynamicAttributeMetadataResponse(
                rs.getLong("attribute_id"),
                rs.getString("name"),
                rs.getString("data_type"),
                rs.getBoolean("is_required")
        ));
    }

    @Transactional
    public DynamicAttributeMetadataResponse createType(String resource, DynamicAttributeTypeRequest request) {
        requireAdmin();
        AttributeTables tables = tables(resource);
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO %s (name, data_type)
                VALUES (:name, :dataType)
                ON CONFLICT (name) DO UPDATE SET data_type = EXCLUDED.data_type
                RETURNING attribute_id
                """.formatted(tables.attributeTypes()), Map.of(
                "name", request.name().trim(),
                "dataType", request.dataType()
        ), Long.class);
        return new DynamicAttributeMetadataResponse(id, request.name().trim(), request.dataType(), false);
    }

    @Transactional
    public DynamicAttributeMetadataResponse assignToCategory(String resource, Long categoryId, AssignAttributeRequest request) {
        requireAdmin();
        AttributeTables tables = tables(resource);
        jdbcTemplate.update("""
                INSERT INTO %s (category_id, attribute_id, is_required)
                VALUES (:categoryId, :attributeId, :required)
                ON CONFLICT (category_id, attribute_id) DO UPDATE SET is_required = EXCLUDED.is_required
                """.formatted(tables.categoryAttributes()), Map.of(
                "categoryId", categoryId,
                "attributeId", request.attributeId(),
                "required", Boolean.TRUE.equals(request.required())
        ));
        return categorySchema(resource, categoryId).stream()
                .filter(attribute -> attribute.id().equals(request.attributeId()))
                .findFirst()
                .orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<DynamicAttributeMetadataResponse> rankTypes() {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("personnel:read")) {
            throw new AccessDeniedException("Access denied");
        }
        return jdbcTemplate.query("""
                SELECT attribute_id, name, data_type, FALSE AS is_required
                FROM rank_attribute_types
                ORDER BY name
                """, Map.of(), (rs, rowNum) -> new DynamicAttributeMetadataResponse(
                rs.getLong("attribute_id"),
                rs.getString("name"),
                rs.getString("data_type"),
                rs.getBoolean("is_required")
        ));
    }

    @Transactional
    public DynamicAttributeMetadataResponse createRankType(DynamicAttributeTypeRequest request) {
        requireAdmin();
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO rank_attribute_types (name, data_type)
                VALUES (:name, :dataType)
                ON CONFLICT (name) DO UPDATE SET data_type = EXCLUDED.data_type
                RETURNING attribute_id
                """, Map.of("name", request.name().trim(), "dataType", request.dataType()), Long.class);
        return new DynamicAttributeMetadataResponse(id, request.name().trim(), request.dataType(), false);
    }

    @Transactional
    public DynamicAttributeMetadataResponse assignToRank(Long rankId, AssignAttributeRequest request) {
        requireAdmin();
        jdbcTemplate.update("""
                INSERT INTO rank_type_attributes (rank_id, attribute_id, is_required)
                VALUES (:rankId, :attributeId, :required)
                ON CONFLICT (rank_id, attribute_id) DO UPDATE SET is_required = EXCLUDED.is_required
                """, Map.of(
                "rankId", rankId,
                "attributeId", request.attributeId(),
                "required", Boolean.TRUE.equals(request.required())
        ));
        return rankSchema(rankId).stream()
                .filter(attribute -> attribute.id().equals(request.attributeId()))
                .findFirst()
                .orElseThrow();
    }

    private void requireAdmin() {
        UserContext user = userContextProvider.current();
        if (!user.hasRole(RoleCode.ADMIN_DISTRICT)) {
            throw new AccessDeniedException("Only district administrator can manage dynamic attributes");
        }
    }

    private void checkRead(String resource) {
        UserContext user = userContextProvider.current();
        String permission = switch (resource.toLowerCase(Locale.ROOT)) {
            case "equipment" -> "equipment:read";
            case "weapons", "weapon" -> "weapon:read";
            default -> throw new IllegalArgumentException("Unsupported attribute resource: " + resource);
        };
        if (!user.hasPermission(permission)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private AttributeTables tables(String resource) {
        return switch (resource.toLowerCase(Locale.ROOT)) {
            case "equipment" -> new AttributeTables(
                    "equipment_attribute_types",
                    "equipment_category_attributes"
            );
            case "weapons", "weapon" -> new AttributeTables(
                    "weapon_attribute_types",
                    "weapon_category_attributes"
            );
            default -> throw new IllegalArgumentException("Unsupported attribute resource: " + resource);
        };
    }

    private record AttributeTables(String attributeTypes, String categoryAttributes) {
    }
}
