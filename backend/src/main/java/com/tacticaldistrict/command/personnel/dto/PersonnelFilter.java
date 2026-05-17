package com.tacticaldistrict.command.personnel.dto;

public record PersonnelFilter(
        String search,
        Long unitId,
        Long subdivisionId,
        String rank,
        Long specialtyId
) {
}
