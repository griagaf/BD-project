package com.tacticaldistrict.command.intelligence.mapper;

import com.tacticaldistrict.command.intelligence.dto.QueryResultDto;
import com.tacticaldistrict.command.intelligence.model.QueryTemplate;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class QueryResultMapper {

    public QueryResultDto toResult(QueryTemplate template, String previewCommand, List<Map<String, Object>> rows) {
        List<Map<String, Object>> normalizedRows = rows.stream()
                .map(this::normalize)
                .toList();
        List<String> columns = normalizedRows.isEmpty() ? List.of() : new ArrayList<>(normalizedRows.get(0).keySet());
        return new QueryResultDto(
                template.name(),
                previewCommand,
                columns,
                normalizedRows,
                normalizedRows.size(),
                Instant.now()
        );
    }

    public byte[] toCsv(QueryResultDto result) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append(String.join(",", result.columns())).append("\n");
        for (Map<String, Object> row : result.rows()) {
            csv.append(result.columns().stream()
                    .map(column -> csvCell(row.get(column)))
                    .collect(java.util.stream.Collectors.joining(",")))
                    .append("\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Map<String, Object> normalize(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        row.forEach((key, value) -> normalized.put(key.toLowerCase(java.util.Locale.ROOT), value));
        return normalized;
    }

    private String csvCell(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString().replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}
