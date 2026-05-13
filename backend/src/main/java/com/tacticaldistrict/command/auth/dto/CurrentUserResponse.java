package com.tacticaldistrict.command.auth.dto;

import com.tacticaldistrict.command.user.dto.CommandAssignmentResponse;
import java.util.List;
import java.util.Set;

public record CurrentUserResponse(
        Long userId,
        Long soldierId,
        String username,
        String displayName,
        Set<String> roles,
        Set<String> effectiveRoles,
        List<CommandAssignmentResponse> assignments,
        Set<String> permissions,
        boolean accessSimulationActive
) {
}

