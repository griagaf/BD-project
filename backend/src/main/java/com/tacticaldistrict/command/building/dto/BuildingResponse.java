package com.tacticaldistrict.command.building.dto;

public record BuildingResponse(
        Long id,
        String name,
        Long unitId,
        String unitName,
        Boolean assignable,
        Long subdivisionsCount,
        String status,
        java.util.List<BuildingAssignmentResponse> assignedSubdivisions
) {
}
