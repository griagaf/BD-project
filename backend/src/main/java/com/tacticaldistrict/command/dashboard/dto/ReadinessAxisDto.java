package com.tacticaldistrict.command.dashboard.dto;

public record ReadinessAxisDto(
        String key,
        String label,
        Integer score,
        String status
) {
}
