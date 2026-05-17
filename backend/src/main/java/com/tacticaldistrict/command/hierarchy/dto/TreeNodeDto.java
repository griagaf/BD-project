package com.tacticaldistrict.command.hierarchy.dto;

import java.util.List;

public record TreeNodeDto(
        String id,
        String type,
        Long objectId,
        String label,
        String subtitle,
        String status,
        Integer level,
        Boolean hasChildren,
        Boolean childrenLoaded,
        Integer childrenCount,
        CommanderResponse commander,
        TreeNodeMetricsDto metrics,
        List<TreeNodeDto> children
) {
}
