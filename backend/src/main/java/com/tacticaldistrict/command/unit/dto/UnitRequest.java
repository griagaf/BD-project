package com.tacticaldistrict.command.unit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UnitRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull Long formationId,
        Long locationId,
        Long commanderId
) {
}
