package com.tacticaldistrict.command.common.attribute;

import jakarta.validation.constraints.NotNull;

public record DynamicAttributeValueRequest(
        @NotNull Long attributeId,
        String value
) {
}
