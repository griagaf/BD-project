package com.tacticaldistrict.command.equipment.dto;

import com.tacticaldistrict.command.common.attribute.DynamicAttributeValueRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record EquipmentTypeRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Long categoryId,
        @Size(max = 255) String purpose,
        @Min(0) Integer crewSize,
        @Min(0) BigDecimal weightTons,
        @Min(0) Integer maxSpeedKmh,
        @Min(0) Integer operationalRangeKm,
        @Min(1900) @Max(2100) Integer adoptionYear,
        @Size(max = 160) String manufacturer,
        String description,
        List<DynamicAttributeValueRequest> attributes
) {
}
