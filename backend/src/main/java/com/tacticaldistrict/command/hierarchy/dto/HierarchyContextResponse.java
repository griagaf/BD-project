package com.tacticaldistrict.command.hierarchy.dto;

import java.util.List;

public record HierarchyContextResponse(
        List<ActionItemResponse> actions,
        List<String> alerts,
        TreeNodeMetricsDto statistics
) {
}
