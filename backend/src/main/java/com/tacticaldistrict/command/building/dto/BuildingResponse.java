package com.tacticaldistrict.command.building.dto;

public record BuildingResponse(
        Long id,
        String name,
        Long unitId,
        String unitName,
        Long subdivisionsCount,
        String status
) {
}
