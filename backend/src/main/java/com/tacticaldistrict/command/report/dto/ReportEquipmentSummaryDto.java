package com.tacticaldistrict.command.report.dto;

import java.util.List;

public record ReportEquipmentSummaryDto(
        long totalQuantity,
        long typesCount,
        long unitsWithoutEquipment,
        List<ResourceQuantityDto> topEquipment,
        List<String> missingEquipmentWarnings
) {
}
