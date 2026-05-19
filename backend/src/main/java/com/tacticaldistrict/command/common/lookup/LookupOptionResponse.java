package com.tacticaldistrict.command.common.lookup;

public record LookupOptionResponse(
        Long id,
        String label,
        String description,
        String type,
        String parentLabel,
        String disabledReason
) {
}
