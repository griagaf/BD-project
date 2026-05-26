package com.tacticaldistrict.command.intelligence.application.port;

import com.tacticaldistrict.command.intelligence.repository.QueryDefinition;
import java.util.List;
import java.util.Map;

public interface QueryExecutionPort {

    List<Map<String, Object>> execute(QueryDefinition definition);
}
