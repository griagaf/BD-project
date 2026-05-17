package com.tacticaldistrict.command.report.dto;

import java.util.List;
import java.util.Map;

public record ReportSpecialtySummaryDto(
        long totalSpecialties,
        long coveredSpecialties,
        long missingSpecialties,
        List<String> missingSpecialtyNames,
        Map<String, Long> topSpecialties
) {
}
