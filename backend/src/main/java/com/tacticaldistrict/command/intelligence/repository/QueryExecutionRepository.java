package com.tacticaldistrict.command.intelligence.repository;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QueryExecutionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> execute(QueryDefinition definition) {
        return jdbcTemplate.queryForList(definition.sql(), definition.parameters());
    }
}
