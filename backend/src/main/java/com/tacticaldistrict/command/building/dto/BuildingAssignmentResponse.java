package com.tacticaldistrict.command.building.dto;

public record BuildingAssignmentResponse(
        Long id,
        String name,
        String type,
        Long unitId,
        String unitName
) {
}
