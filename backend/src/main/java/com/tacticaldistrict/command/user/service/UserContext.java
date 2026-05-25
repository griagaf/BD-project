package com.tacticaldistrict.command.user.service;

import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.security.model.ObjectType;
import java.util.Arrays;
import java.util.Set;

public record UserContext(
        Long userId,
        Long soldierId,
        String username,
        String displayName,
        Set<RoleCode> roles,
        Set<String> permissions,
        boolean accessSimulationActive,
        ObjectType simulationScopeType,
        Long simulationScopeId
) {

    public boolean hasRole(RoleCode role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(RoleCode... requiredRoles) {
        return Arrays.stream(requiredRoles).anyMatch(roles::contains);
    }

    public boolean hasPermission(String permission) {
        if (roles.contains(RoleCode.ADMIN_DISTRICT)) {
            return true;
        }
        return permissions.contains(permission);
    }

    public boolean hasSimulationScope() {
        return accessSimulationActive && simulationScopeType != null && simulationScopeId != null;
    }
}
