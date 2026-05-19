package com.tacticaldistrict.command.weapon.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WeaponTypeRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Long categoryId,
        @Size(max = 255) String purpose,
        @Size(max = 80) String caliber,
        @Min(0) Integer effectiveRangeM,
        @Min(1900) @Max(2100) Integer adoptionYear,
        @Size(max = 160) String manufacturer,
        String description
) {
}
