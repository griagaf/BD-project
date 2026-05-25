package com.tacticaldistrict.command.common.dto;

public record AttributeValueResponse(
        Long id,
        String name,
        String dataType,
        String displayValue
) {
}
