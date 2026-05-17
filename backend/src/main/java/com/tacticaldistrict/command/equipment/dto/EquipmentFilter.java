package com.tacticaldistrict.command.equipment.dto;

public record EquipmentFilter(
        String search,
        Long unitId,
        Long categoryId,
        Long typeId
) {
}
