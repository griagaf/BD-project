package com.tacticaldistrict.command.dashboard.dto;

import java.util.List;

public record ReadinessDto(
        Integer overall,
        List<ReadinessAxisDto> axes
) {
}
