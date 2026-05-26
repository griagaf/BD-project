package com.tacticaldistrict.command.intelligence.service;

import com.tacticaldistrict.command.intelligence.dto.ExecuteQueryRequest;
import com.tacticaldistrict.command.intelligence.dto.QueryParameterMetadataDto;
import com.tacticaldistrict.command.intelligence.model.QueryTemplate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class QueryParameterResolver {

    public void validate(QueryTemplate template, ExecuteQueryRequest request) {
        Map<String, Object> parameters = parameters(request);
        for (QueryParameterMetadataDto parameter : template.parameters()) {
            if (Boolean.TRUE.equals(parameter.required()) && isBlank(parameters.get(parameter.name()))) {
                throw new IllegalArgumentException("Required query parameter is missing: " + parameter.name());
            }
        }
    }

    public Map<String, Object> parameters(ExecuteQueryRequest request) {
        return request == null || request.parameters() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request.parameters());
    }

    private boolean isBlank(Object value) {
        return value == null || value.toString().isBlank();
    }
}
