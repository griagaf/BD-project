package com.tacticaldistrict.command.security.access;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchemaIntrospectionService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Map<String, Boolean> relationCache = new ConcurrentHashMap<>();

    public boolean relationExists(String relationName) {
        return relationCache.computeIfAbsent(relationName, this::lookupRelation);
    }

    private boolean lookupRelation(String relationName) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = current_schema()
                      AND table_name = :relationName
                    UNION ALL
                    SELECT 1
                    FROM information_schema.views
                    WHERE table_schema = current_schema()
                      AND table_name = :relationName
                )
                """, Map.of("relationName", relationName), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }
}
