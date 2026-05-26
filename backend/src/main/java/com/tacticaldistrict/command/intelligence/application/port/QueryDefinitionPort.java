package com.tacticaldistrict.command.intelligence.application.port;

import com.tacticaldistrict.command.intelligence.dto.ExecuteQueryRequest;
import com.tacticaldistrict.command.intelligence.model.QueryTemplate;
import com.tacticaldistrict.command.intelligence.repository.QueryDefinition;

public interface QueryDefinitionPort {

    QueryDefinition build(QueryTemplate template, ExecuteQueryRequest request);
}
