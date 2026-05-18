package com.tacticaldistrict.command.report.dto;

public record ReportCommanderDto(
        Long personnelId,
        String fullName,
        String rankName,
        String position,
        String objectName
) {
}
