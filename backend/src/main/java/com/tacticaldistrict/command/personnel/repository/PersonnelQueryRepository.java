package com.tacticaldistrict.command.personnel.repository;

import com.tacticaldistrict.command.common.dto.PageResponse;
import com.tacticaldistrict.command.common.dto.AttributeValueResponse;
import com.tacticaldistrict.command.personnel.dto.ChainOfCommandNodeResponse;
import com.tacticaldistrict.command.personnel.dto.PersonnelFilter;
import com.tacticaldistrict.command.personnel.dto.PersonnelProfileResponse;
import com.tacticaldistrict.command.personnel.dto.PersonnelResponse;
import com.tacticaldistrict.command.personnel.dto.RankResponse;
import com.tacticaldistrict.command.personnel.dto.SpecialtyResponse;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.user.service.UserContext;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PersonnelQueryRepository {

    private static final List<String> FORMATION_ASSIGNMENT_TYPES = List.of(
            "DISTRICT", "FORMATION", "ARMY", "CORPS", "DIVISION", "BRIGADE"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PageResponse<PersonnelResponse> search(UserContext user, PersonnelFilter filter, Pageable pageable) {
        QueryParts query = buildSearchQuery(user, filter, null);
        String orderBy = orderBy(pageable.getSort());

        Map<String, Object> params = new HashMap<>(query.params());
        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        List<PersonnelResponse> content = jdbcTemplate.query(
                query.selectSql() + " " + orderBy + " LIMIT :limit OFFSET :offset",
                params,
                this::mapPersonnel
        );
        Long total = jdbcTemplate.queryForObject(query.countSql(), query.params(), Long.class);
        long totalElements = total == null ? 0 : total;
        int totalPages = pageable.getPageSize() == 0
                ? 0
                : (int) Math.ceil((double) totalElements / pageable.getPageSize());

        return new PageResponse<>(
                content,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                totalElements,
                totalPages,
                pageable.getPageNumber() == 0,
                pageable.getPageNumber() + 1 >= totalPages
        );
    }

    public Optional<PersonnelResponse> findById(UserContext user, Long personnelId) {
        PersonnelFilter filter = new PersonnelFilter(null, null, null, null, null);
        QueryParts query = buildSearchQuery(user, filter, personnelId);

        List<PersonnelResponse> rows = jdbcTemplate.query(
                query.selectSql(),
                query.params(),
                this::mapPersonnel
        );
        return rows.stream().findFirst();
    }

    public PersonnelProfileResponse profile(PersonnelResponse personnel) {
        List<ChainOfCommandNodeResponse> chain = chainOfCommand(personnel.id());
        String formationName = chain.stream()
                .filter(node -> "FORMATION".equals(node.objectType()))
                .map(ChainOfCommandNodeResponse::objectName)
                .findFirst()
                .orElse(null);
        String assignmentPath = chain.stream()
                .map(ChainOfCommandNodeResponse::objectName)
                .reduce((left, right) -> left + " / " + right)
                .orElse(personnel.subdivisionName());

        return new PersonnelProfileResponse(
                personnel,
                formationName,
                assignmentPath,
                LocalDate.now(),
                rankAttributes(personnel.id()),
                chain
        );
    }

    private List<AttributeValueResponse> rankAttributes(Long personnelId) {
        return jdbcTemplate.query("""
                SELECT rat.attribute_id,
                       rat.name,
                       rat.data_type,
                       COALESCE(
                           rav.value_text,
                           trim(to_char(rav.value_number, 'FM999999990.99')),
                           to_char(rav.value_date, 'YYYY-MM-DD'),
                           CASE WHEN rav.value_boolean IS NULL THEN NULL ELSE rav.value_boolean::TEXT END
                       ) AS display_value
                FROM rank_attribute_values rav
                JOIN rank_attribute_types rat ON rat.attribute_id = rav.attribute_id
                WHERE rav.personnel_id = :personnelId
                  AND COALESCE(
                      rav.value_text,
                      rav.value_number::TEXT,
                      rav.value_date::TEXT,
                      rav.value_boolean::TEXT
                  ) IS NOT NULL
                ORDER BY rat.attribute_id
                """, Map.of("personnelId", personnelId), (rs, rowNum) -> new AttributeValueResponse(
                rs.getLong("attribute_id"),
                rs.getString("name"),
                rs.getString("data_type"),
                rs.getString("display_value")
        ));
    }

    public List<ChainOfCommandNodeResponse> chainOfCommand(Long personnelId) {
        return jdbcTemplate.query("""
                SELECT 'FORMATION' AS object_type,
                       mf.formation_id AS object_id,
                       mf.name AS object_name,
                       mf.commander_id AS commander_personnel_id,
                       trim(concat(cp.last_name, ' ', cp.first_name, ' ', coalesce(cp.middle_name, ''))) AS commander_name,
                       1 AS sort_order
                FROM personnel p
                JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
                JOIN military_units mu ON mu.unit_id = s.unit_id
                JOIN military_formations mf ON mf.formation_id = mu.formation_id
                LEFT JOIN personnel cp ON cp.personnel_id = mf.commander_id
                WHERE p.personnel_id = :personnelId

                UNION ALL

                SELECT 'MILITARY_UNIT',
                       mu.unit_id,
                       mu.name,
                       mu.commander_id,
                       trim(concat(cp.last_name, ' ', cp.first_name, ' ', coalesce(cp.middle_name, ''))),
                       2
                FROM personnel p
                JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
                JOIN military_units mu ON mu.unit_id = s.unit_id
                LEFT JOIN personnel cp ON cp.personnel_id = mu.commander_id
                WHERE p.personnel_id = :personnelId

                UNION ALL

                SELECT CASE s.type
                           WHEN 'Батальон' THEN 'BATTALION'
                           WHEN 'Рота' THEN 'COMPANY'
                           WHEN 'Взвод' THEN 'PLATOON'
                           WHEN 'Отделение' THEN 'SQUAD'
                           ELSE 'PLATOON'
                       END,
                       s.subdivision_id,
                       s.name,
                       s.commander_id,
                       trim(concat(cp.last_name, ' ', cp.first_name, ' ', coalesce(cp.middle_name, ''))),
                       3
                FROM personnel p
                JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
                LEFT JOIN personnel cp ON cp.personnel_id = s.commander_id
                WHERE p.personnel_id = :personnelId

                ORDER BY sort_order
                """, Map.of("personnelId", personnelId), (rs, rowNum) -> new ChainOfCommandNodeResponse(
                rs.getString("object_type"),
                rs.getLong("object_id"),
                rs.getString("object_name"),
                rs.getObject("commander_personnel_id", Long.class),
                rs.getString("commander_name")
        ));
    }

    public String subdivisionType(Long subdivisionId) {
        return jdbcTemplate.query("""
                SELECT s.type
                FROM subdivisions s
                WHERE s.subdivision_id = :subdivisionId
                """, Map.of("subdivisionId", subdivisionId), rs -> rs.next() ? rs.getString("type") : null);
    }

    private QueryParts buildSearchQuery(UserContext user, PersonnelFilter filter, Long personnelId) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");

        if (personnelId != null) {
            where.append(" AND p.personnel_id = :personnelId ");
            params.put("personnelId", personnelId);
        }
        if (filter.search() != null && !filter.search().isBlank()) {
            where.append("""
                    AND (
                        lower(p.last_name) LIKE :search
                        OR lower(p.first_name) LIKE :search
                        OR lower(coalesce(p.middle_name, '')) LIKE :search
                        OR lower(p.personal_number) LIKE :search
                    )
                    """);
            params.put("search", "%" + filter.search().trim().toLowerCase() + "%");
        }
        if (filter.unitId() != null) {
            where.append(" AND mu.unit_id = :unitId ");
            params.put("unitId", filter.unitId());
        }
        if (filter.subdivisionId() != null) {
            where.append(" AND s.subdivision_id = :subdivisionId ");
            params.put("subdivisionId", filter.subdivisionId());
        }
        if (filter.rank() != null && !filter.rank().isBlank()) {
            where.append(" AND lower(mr.name) = :rank ");
            params.put("rank", filter.rank().trim().toLowerCase());
        }
        if (filter.specialtyId() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM personnel_specialties fps WHERE fps.personnel_id = p.personnel_id AND fps.specialty_id = :specialtyId) ");
            params.put("specialtyId", filter.specialtyId());
        }

        appendScope(user, where, params);

        String fromSql = """
                FROM personnel p
                JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
                JOIN military_units mu ON mu.unit_id = s.unit_id
                JOIN military_formations mf ON mf.formation_id = mu.formation_id
                LEFT JOIN v_formation_closure fc ON fc.descendant_formation_id = mu.formation_id
                LEFT JOIN personnel_ranks pr ON pr.personnel_id = p.personnel_id
                LEFT JOIN military_ranks mr ON mr.rank_id = pr.rank_id
                LEFT JOIN personnel_specialties ps ON ps.personnel_id = p.personnel_id
                LEFT JOIN specialties sp ON sp.specialty_id = ps.specialty_id
                """;

        String groupBy = """
                GROUP BY p.personnel_id, p.last_name, p.first_name, p.middle_name,
                         p.personal_number, p.birth_date, p.service_start, p.subdivision_id,
                         s.name, mu.unit_id, mu.name, mr.rank_id, mr.name, mr.category
                """;

        String selectSql = """
                SELECT p.personnel_id,
                       p.last_name,
                       p.first_name,
                       p.middle_name,
                       p.personal_number,
                       p.birth_date,
                       p.service_start,
                       p.subdivision_id,
                       s.name AS subdivision_name,
                       mu.unit_id,
                       mu.name AS unit_name,
                       mr.rank_id,
                       mr.name AS rank_name,
                       mr.category AS rank_category,
                       string_agg(DISTINCT concat(sp.specialty_id, ':', sp.name), '|' ORDER BY concat(sp.specialty_id, ':', sp.name))
                               FILTER (WHERE sp.specialty_id IS NOT NULL) AS specialties
                """ + fromSql + where + groupBy;

        String countSql = "SELECT COUNT(*) FROM (SELECT p.personnel_id " + fromSql + where + " GROUP BY p.personnel_id) scoped_personnel";
        return new QueryParts(selectSql, countSql, params);
    }

    private void appendScope(UserContext user, StringBuilder where, Map<String, Object> params) {
        if (user.hasAnyRole(RoleCode.ADMIN_DISTRICT, RoleCode.STAFF_ANALYST)) {
            return;
        }

        if (user.hasSimulationScope()) {
            appendSimulationScope(user, where, params);
            return;
        }

        where.append("""
                AND (
                    p.personnel_id = :scopeSoldierId
                    OR EXISTS (
                        SELECT 1
                        FROM command_assignments ca
                        WHERE ca.soldier_id = :scopeSoldierId
                          AND ca.starts_at <= CURRENT_DATE
                          AND (ca.ends_at IS NULL OR ca.ends_at >= CURRENT_DATE)
                          AND (
                              (ca.object_type IN ('PERSONNEL', 'SELF') AND ca.object_id = p.personnel_id)
                              OR (ca.object_type = 'MILITARY_UNIT' AND ca.object_id = mu.unit_id)
                              OR (ca.object_type IN (:formationAssignmentTypes) AND ca.object_id = fc.root_formation_id)
                          )
                    )
                    OR EXISTS (
                        WITH RECURSIVE sub_tree AS (
                            SELECT ca.object_id AS subdivision_id
                            FROM command_assignments ca
                            WHERE ca.soldier_id = :scopeSoldierId
                              AND ca.starts_at <= CURRENT_DATE
                              AND (ca.ends_at IS NULL OR ca.ends_at >= CURRENT_DATE)
                              AND ca.object_type IN ('BATTALION', 'COMPANY', 'PLATOON', 'SQUAD')
                            UNION ALL
                            SELECT child.subdivision_id
                            FROM subdivisions child
                            JOIN sub_tree parent ON child.parent_id = parent.subdivision_id
                        )
                        SELECT 1
                        FROM sub_tree st
                        WHERE st.subdivision_id = s.subdivision_id
                    )
                )
                """);
        params.put("scopeSoldierId", user.soldierId());
        params.put("formationAssignmentTypes", FORMATION_ASSIGNMENT_TYPES);
    }

    private void appendSimulationScope(UserContext user, StringBuilder where, Map<String, Object> params) {
        ObjectType scopeType = user.simulationScopeType();
        Long scopeId = user.simulationScopeId();
        if (scopeType == null || scopeId == null || scopeType == ObjectType.DISTRICT) {
            return;
        }
        params.put("simulationScopeId", scopeId);
        switch (scopeType) {
            case SELF, PERSONNEL -> where.append(" AND p.personnel_id = :simulationScopeId ");
            case MILITARY_UNIT -> where.append(" AND mu.unit_id = :simulationScopeId ");
            case FORMATION, ARMY, CORPS, DIVISION, BRIGADE -> where.append("""
                    AND EXISTS (
                        SELECT 1
                        FROM v_formation_closure simulation_fc
                        WHERE simulation_fc.root_formation_id = :simulationScopeId
                          AND simulation_fc.descendant_formation_id = mu.formation_id
                    )
                    """);
            case BATTALION, COMPANY, PLATOON, SQUAD -> where.append("""
                    AND EXISTS (
                        WITH RECURSIVE simulation_sub_tree AS (
                            SELECT subdivision_id
                            FROM subdivisions
                            WHERE subdivision_id = :simulationScopeId
                            UNION ALL
                            SELECT child.subdivision_id
                            FROM subdivisions child
                            JOIN simulation_sub_tree parent ON child.parent_id = parent.subdivision_id
                        )
                        SELECT 1
                        FROM simulation_sub_tree st
                        WHERE st.subdivision_id = s.subdivision_id
                    )
                    """);
            default -> where.append(" AND 1 = 0 ");
        }
    }

    private String orderBy(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return "ORDER BY p.last_name ASC, p.first_name ASC";
        }

        List<String> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            String column = switch (order.getProperty()) {
                case "lastName" -> "p.last_name";
                case "firstName" -> "p.first_name";
                case "personalNumber" -> "p.personal_number";
                case "rank" -> "mr.name";
                case "unitName" -> "mu.name";
                case "subdivisionName" -> "s.name";
                case "serviceStart" -> "p.service_start";
                default -> null;
            };
            if (column != null) {
                orders.add(column + (order.isAscending() ? " ASC" : " DESC"));
            }
        }

        return orders.isEmpty()
                ? "ORDER BY p.last_name ASC, p.first_name ASC"
                : "ORDER BY " + String.join(", ", orders);
    }

    private PersonnelResponse mapPersonnel(ResultSet rs, int rowNum) throws SQLException {
        String fullName = "%s %s %s".formatted(
                rs.getString("last_name"),
                rs.getString("first_name"),
                rs.getString("middle_name") == null ? "" : rs.getString("middle_name")
        ).trim();
        Long rankId = rs.getObject("rank_id", Long.class);

        return new PersonnelResponse(
                rs.getLong("personnel_id"),
                rs.getString("last_name"),
                rs.getString("first_name"),
                rs.getString("middle_name"),
                fullName,
                rs.getString("personal_number"),
                rs.getObject("birth_date", LocalDate.class),
                rs.getObject("service_start", LocalDate.class),
                rs.getLong("subdivision_id"),
                rs.getString("subdivision_name"),
                rs.getLong("unit_id"),
                rs.getString("unit_name"),
                rankId == null ? null : new RankResponse(rankId, rs.getString("rank_name"), rs.getString("rank_category")),
                specialties(rs.getString("specialties"))
        );
    }

    private List<SpecialtyResponse> specialties(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        List<SpecialtyResponse> result = new ArrayList<>();
        for (String item : value.split("\\|")) {
            String[] parts = item.split(":", 2);
            if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                result.add(new SpecialtyResponse(Long.valueOf(parts[0]), parts[1]));
            }
        }
        return result;
    }

    private record QueryParts(
            String selectSql,
            String countSql,
            Map<String, Object> params
    ) {
    }
}
