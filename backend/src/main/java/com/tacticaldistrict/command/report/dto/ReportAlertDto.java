package com.tacticaldistrict.command.report.dto;

public record ReportAlertDto(
        String alertId,
        String type,
        String severity,
        String title,
        String message,
        String objectType,
        Long objectId
) {
}
