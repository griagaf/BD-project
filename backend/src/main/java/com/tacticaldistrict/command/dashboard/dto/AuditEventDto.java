package com.tacticaldistrict.command.dashboard.dto;

import java.time.Instant;

public record AuditEventDto(
        Long id,
        String eventType,
        String objectType,
        Long objectId,
        String actor,
        String message,
        Instant createdAt
) {
}
