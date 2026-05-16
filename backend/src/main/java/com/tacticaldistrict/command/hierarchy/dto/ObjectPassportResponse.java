package com.tacticaldistrict.command.hierarchy.dto;

import java.util.List;
import java.util.Map;

public record ObjectPassportResponse(
        String objectType,
        Long objectId,
        String name,
        String subtitle,
        String status,
        CommanderResponse commander,
        TreeNodeMetricsDto metrics,
        List<TreeNodeDto> breadcrumbs,
        Map<String, Object> details
) {
}
