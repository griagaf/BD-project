package com.tacticaldistrict.command.hierarchy.dto;

public record CommanderResponse(
        Long personnelId,
        String fullName,
        String rankName
) {
}
