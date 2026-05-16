package com.tacticaldistrict.command.hierarchy.dto;

public record TreeNodeMetricsDto(
        Integer personnelCount,
        Integer unitCount,
        Integer subdivisionCount,
        Integer equipmentCount,
        Integer weaponCount,
        Integer readinessScore
) {
}
