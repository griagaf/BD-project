package com.tacticaldistrict.command.hierarchy.dto;

public record SubdivisionResponse(
        Long id,
        String name,
        String type,
        Long unitId,
        Long parentId,
        CommanderResponse commander
) {
}
