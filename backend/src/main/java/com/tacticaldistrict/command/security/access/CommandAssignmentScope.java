package com.tacticaldistrict.command.security.access;

import com.tacticaldistrict.command.security.model.ObjectType;
import java.time.LocalDate;

public record CommandAssignmentScope(
        Long assignmentId,
        Long soldierId,
        ObjectType objectType,
        Long objectId,
        LocalDate startsAt,
        LocalDate endsAt,
        boolean primary
) {
}
