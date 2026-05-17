package com.tacticaldistrict.command.alert.dto;

public record AlertActionDto(
        String label,
        String route,
        String queryTemplate
) {
}
