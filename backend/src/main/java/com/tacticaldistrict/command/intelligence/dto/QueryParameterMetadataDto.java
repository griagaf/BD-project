package com.tacticaldistrict.command.intelligence.dto;

import java.util.List;

public record QueryParameterMetadataDto(
        String name,
        String label,
        String type,
        Boolean required,
        String placeholder,
        List<String> options
) {
}
