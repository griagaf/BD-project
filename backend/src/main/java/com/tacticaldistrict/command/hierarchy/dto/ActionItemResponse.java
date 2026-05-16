package com.tacticaldistrict.command.hierarchy.dto;

public record ActionItemResponse(
        String code,
        String label,
        String path,
        Boolean enabled
) {
}
