package com.tacticaldistrict.command.equipment.dto;

import java.math.BigDecimal;
import java.util.List;

public record EquipmentTypePassportResponse(
        Long id,
        String name,
        Long categoryId,
        String categoryName,
        String purpose,
        Integer crewSize,
        BigDecimal weightTons,
        Integer maxSpeedKmh,
        Integer operationalRangeKm,
        Integer adoptionYear,
        String manufacturer,
        String description,
        Long totalQuantity,
        Long unitsCount,
        List<UnitEquipmentResponse> distribution
) {
}
