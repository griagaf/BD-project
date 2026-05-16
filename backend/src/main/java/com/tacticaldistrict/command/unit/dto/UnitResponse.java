package com.tacticaldistrict.command.unit.dto;

import com.tacticaldistrict.command.hierarchy.dto.CommanderResponse;

public record UnitResponse(
        Long id,
        String name,
        Long formationId,
        String formationName,
        Long locationId,
        String locationName,
        CommanderResponse commander
) {
}
