package com.tacticaldistrict.command.equipment.dto;

public record UnitEquipmentResponse(
        Long unitId,
        String unitName,
        Long typeId,
        String typeName,
        Long categoryId,
        String categoryName,
        Integer quantity,
        String status
) {
}
