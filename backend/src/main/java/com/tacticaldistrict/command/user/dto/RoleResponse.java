package com.tacticaldistrict.command.user.dto;

public record RoleResponse(
        Long id,
        String code,
        String name,
        String description
) {
}
