package com.tacticaldistrict.command.report.dto;

public record ReportReadinessDto(
        int overall,
        int personnel,
        int equipment,
        int weapons,
        int specialists,
        int infrastructure,
        String status
) {
}
