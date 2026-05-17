package com.tacticaldistrict.command.hierarchy.dto;

import jakarta.validation.constraints.NotNull;

public record CommanderAssignmentRequest(
        @NotNull Long commanderId
) {
}
