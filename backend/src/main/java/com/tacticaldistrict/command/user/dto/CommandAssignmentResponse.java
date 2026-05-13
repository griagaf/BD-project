package com.tacticaldistrict.command.user.dto;

import java.time.LocalDate;

public record CommandAssignmentResponse(
        Long assignmentId,
        String objectType,
        Long objectId,
        LocalDate startsAt,
        LocalDate endsAt,
        boolean primary
) {
}

