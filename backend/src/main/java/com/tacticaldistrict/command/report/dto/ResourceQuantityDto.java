package com.tacticaldistrict.command.report.dto;

public record ResourceQuantityDto(
        String typeName,
        String categoryName,
        long quantity
) {
}
