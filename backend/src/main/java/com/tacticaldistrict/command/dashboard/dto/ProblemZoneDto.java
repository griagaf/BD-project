package com.tacticaldistrict.command.dashboard.dto;

public record ProblemZoneDto(
        String type,
        String label,
        String severity,
        Long count
) {
}
