package com.tacticaldistrict.command.report.dto;

import com.tacticaldistrict.command.report.model.ReportObjectType;
import java.util.List;

public record ReportObjectDto(
        ReportObjectType type,
        Long id,
        String name,
        String parentName,
        String status,
        String location,
        List<String> path
) {
}
