package com.tacticaldistrict.command.building.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BuildingRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Long unitId
) {
}
