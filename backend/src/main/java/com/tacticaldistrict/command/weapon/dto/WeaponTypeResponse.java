package com.tacticaldistrict.command.weapon.dto;

public record WeaponTypeResponse(
        Long id,
        String name,
        Long categoryId,
        String categoryName
) {
}
