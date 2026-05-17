package com.tacticaldistrict.command.weapon.dto;

public record WeaponFilter(
        String search,
        Long unitId,
        Long categoryId,
        Long typeId
) {
}
