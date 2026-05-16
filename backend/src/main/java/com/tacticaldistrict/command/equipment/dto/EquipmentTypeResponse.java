package com.tacticaldistrict.command.equipment.dto;

public record EquipmentTypeResponse(
        Long id,
        String name,
        Long categoryId,
        String categoryName
) {
}
