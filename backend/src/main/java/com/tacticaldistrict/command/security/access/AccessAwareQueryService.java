package com.tacticaldistrict.command.security.access;

import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.user.service.UserContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessAwareQueryService {

    private final AccessControlService accessControlService;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public <T> List<T> queryScoped(
            UserContext user,
            String sql,
            Map<String, ?> parameters,
            ObjectType objectType,
            String idColumn,
            RowMapper<T> rowMapper
    ) {
        ScopedQuery scopedQuery = accessControlService.applyDirectScopeFilter(user, sql, objectType, idColumn);

        Map<String, Object> mergedParameters = new HashMap<>();
        if (parameters != null) {
            mergedParameters.putAll(parameters);
        }
        mergedParameters.putAll(scopedQuery.parameters());

        return jdbcTemplate.query(scopedQuery.sql(), mergedParameters, rowMapper);
    }
}
