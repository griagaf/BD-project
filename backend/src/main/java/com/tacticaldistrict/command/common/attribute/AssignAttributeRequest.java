package com.tacticaldistrict.command.common.attribute;

import jakarta.validation.constraints.NotNull;

public record AssignAttributeRequest(
        @NotNull Long attributeId,
        Boolean required
) {
}
