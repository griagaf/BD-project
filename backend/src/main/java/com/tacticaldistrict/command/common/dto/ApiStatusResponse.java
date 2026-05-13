package com.tacticaldistrict.command.common.dto;

import java.time.Instant;

public record ApiStatusResponse(
        String application,
        String status,
        Instant timestamp
) {
}

