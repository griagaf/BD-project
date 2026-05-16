package com.tacticaldistrict.command.building.dto;

public record BuildingStatisticsResponse(
        Long visibleUnits,
        Long buildings,
        Long assignedBuildings,
        Long emptyBuildings,
        Long overloadedBuildings,
        Integer readinessScore
) {
}
