package com.tacticaldistrict.command.equipment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryQuantityRequest(
        @NotNull @Min(0) Integer quantity
) {
}
