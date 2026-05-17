package com.tacticaldistrict.command.hierarchy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubdivisionRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 50) String type,
        @NotNull Long unitId,
        Long parentId,
        Long commanderId
) {
}
