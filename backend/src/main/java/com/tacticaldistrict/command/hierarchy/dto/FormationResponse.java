package com.tacticaldistrict.command.hierarchy.dto;

import java.time.LocalDate;

public record FormationResponse(
        Long id,
        String name,
        String formationType,
        Long parentId,
        LocalDate formationDate,
        String status,
        CommanderResponse commander
) {
}
