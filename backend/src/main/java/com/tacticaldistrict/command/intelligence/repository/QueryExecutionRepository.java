package com.tacticaldistrict.command.intelligence.repository;

import com.tacticaldistrict.command.intelligence.application.port.QueryExecutionPort;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QueryExecutionRepository implements QueryExecutionPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<Map<String, Object>> execute(QueryDefinition definition) {
        return jdbcTemplate.queryForList(definition.sql(), definition.parameters());
    }
}
