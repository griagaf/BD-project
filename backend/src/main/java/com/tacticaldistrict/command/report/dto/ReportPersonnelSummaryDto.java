package com.tacticaldistrict.command.report.dto;

import java.util.Map;

public record ReportPersonnelSummaryDto(
        long total,
        long officers,
        long enlisted,
        long commanders,
        Map<String, Long> byRank,
        Map<String, Long> bySubdivision
) {
}
