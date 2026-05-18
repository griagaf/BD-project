package com.tacticaldistrict.command.alert.dto;

import java.util.List;
import java.util.Map;

public record TacticalAlertDto(
        String id,
        String type,
        String severity,
        String title,
        String message,
        String objectType,
        Long objectId,
        Map<String, Object> details,
        List<AlertActionDto> actions
) {
}
