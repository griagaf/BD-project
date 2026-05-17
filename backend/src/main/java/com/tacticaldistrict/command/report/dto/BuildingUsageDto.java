package com.tacticaldistrict.command.report.dto;

public record BuildingUsageDto(
        Long buildingId,
        String buildingName,
        String unitName,
        long subdivisionsCount
) {
}
