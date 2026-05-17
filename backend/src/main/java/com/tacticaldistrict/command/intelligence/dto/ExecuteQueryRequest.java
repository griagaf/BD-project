package com.tacticaldistrict.command.intelligence.dto;

import java.util.Map;

public record ExecuteQueryRequest(
        QueryScopeDto scope,
        Map<String, Object> parameters,
        String previewCommand
) {
}
