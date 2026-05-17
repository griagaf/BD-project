package com.tacticaldistrict.command.intelligence.dto;

import java.util.List;
import java.util.Set;

public record QueryTemplateMetadataDto(
        String code,
        String label,
        String description,
        String target,
        List<QueryParameterMetadataDto> parameters,
        Set<String> requiredPermissions,
        String exampleCommand
) {
}
