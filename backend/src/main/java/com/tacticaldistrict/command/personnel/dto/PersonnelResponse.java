package com.tacticaldistrict.command.personnel.dto;

import java.time.LocalDate;
import java.util.List;

public record PersonnelResponse(
        Long id,
        String lastName,
        String firstName,
        String middleName,
        String fullName,
        String personalNumber,
        LocalDate birthDate,
        LocalDate serviceStart,
        Long subdivisionId,
        String subdivisionName,
        Long unitId,
        String unitName,
        RankResponse rank,
        List<SpecialtyResponse> specialties
) {
}
