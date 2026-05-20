package com.tacticaldistrict.command.common.dto;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String details,
        String path,
        String traceId,
        Map<String, String> fieldErrors,
        Map<String, String> validationErrors
) {
}
