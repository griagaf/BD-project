package com.tacticaldistrict.command.weapon.dto;

public record UnitWeaponResponse(
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
