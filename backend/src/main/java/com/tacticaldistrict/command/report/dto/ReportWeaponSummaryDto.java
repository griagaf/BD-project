package com.tacticaldistrict.command.report.dto;

import java.util.List;

public record ReportWeaponSummaryDto(
        long totalQuantity,
        long typesCount,
        long unitsWithoutWeapons,
        List<ResourceQuantityDto> topWeapons,
        List<String> missingWeaponWarnings
) {
}
