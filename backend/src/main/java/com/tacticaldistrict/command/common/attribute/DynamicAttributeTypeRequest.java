package com.tacticaldistrict.command.common.attribute;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DynamicAttributeTypeRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "text|number|date|boolean") String dataType
) {
}
