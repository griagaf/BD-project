package com.tacticaldistrict.command.security.access;

import com.tacticaldistrict.command.security.model.ObjectType;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public Set<Long> formationAssignmentIds() {
        return assignmentIds(ObjectType.DISTRICT, ObjectType.FORMATION, ObjectType.ARMY, ObjectType.CORPS, ObjectType.DIVISION, ObjectType.BRIGADE);
    }

    public Set<Long> unitAssignmentIds() {
        return assignmentIds(ObjectType.MILITARY_UNIT);
    }

    public Set<Long> subdivisionAssignmentIds() {
        return assignmentIds(ObjectType.BATTALION, ObjectType.COMPANY, ObjectType.PLATOON, ObjectType.SQUAD);
    }

    public Set<Long> selfAssignmentIds() {
        return assignmentIds(ObjectType.SELF);
    }

    private Set<Long> assignmentIds(ObjectType... types) {
        Set<ObjectType> requestedTypes = Set.of(types);
        return assignments.stream()
                .filter(assignment -> requestedTypes.contains(assignment.objectType()))
                .map(CommandAssignmentScope::objectId)
                .collect(Collectors.toUnmodifiableSet());
    }
}
