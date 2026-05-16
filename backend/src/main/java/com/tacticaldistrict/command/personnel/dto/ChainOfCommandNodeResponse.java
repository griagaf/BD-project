package com.tacticaldistrict.command.personnel.dto;

public record ChainOfCommandNodeResponse(
        String objectType,
        Long objectId,
        String objectName,
        Long commanderPersonnelId,
        String commanderName
) {
}
