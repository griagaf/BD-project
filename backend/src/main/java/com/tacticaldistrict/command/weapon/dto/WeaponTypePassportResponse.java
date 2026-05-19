package com.tacticaldistrict.command.weapon.dto;

import java.util.List;

public record WeaponTypePassportResponse(
        Long id,
        String name,
        Long categoryId,
        String categoryName,
        String purpose,
        String caliber,
        Integer effectiveRangeM,
        Integer adoptionYear,
        String manufacturer,
        String description,
        Long totalQuantity,
        Long unitsCount,
        List<UnitWeaponResponse> distribution
) {
}
