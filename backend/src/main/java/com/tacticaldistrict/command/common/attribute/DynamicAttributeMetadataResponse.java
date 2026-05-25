package com.tacticaldistrict.command.common.attribute;

public record DynamicAttributeMetadataResponse(
        Long id,
        String name,
        String dataType,
        Boolean required
) {
}
