package com.tacticaldistrict.command.user.dto;

import java.time.Instant;
import java.util.Set;

public record UserAdminResponse(
        Long id,
        String username,
        String displayName,
        Long personnelId,
        boolean active,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt
) {
}
