package com.tacticaldistrict.command.intelligence.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record QueryResultDto(
        String code,
        String previewCommand,
        List<String> columns,
        List<Map<String, Object>> rows,
        Integer rowCount,
        Instant executedAt
) {
}
