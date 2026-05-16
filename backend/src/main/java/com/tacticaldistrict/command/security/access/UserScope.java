package com.tacticaldistrict.command.security.access;

import com.tacticaldistrict.command.security.model.ObjectType;
import java.util.List;

public record UserScope(
        Long userId,
        Long soldierId,
        boolean unrestricted,
        boolean selfOnly,
        List<CommandAssignmentScope> assignments
) {

    public boolean hasDirectAssignment(ObjectType objectType, Long objectId) {
        if (objectType == null || objectId == null) {
            return false;
        }

        return assignments.stream()
                .anyMatch(assignment -> assignment.objectType() == objectType
                        && assignment.objectId().equals(objectId));
    }

    public boolean hasDistrictAssignment() {
        return assignments.stream()
                .anyMatch(assignment -> assignment.objectType() == ObjectType.DISTRICT);
    }
}
