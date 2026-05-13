package com.tacticaldistrict.command.user.service;

import com.tacticaldistrict.command.security.model.RoleCode;
import java.util.Arrays;
import java.util.Set;

public record UserContext(
        Long userId,
        Long soldierId,
        String username,
        String displayName,
        Set<RoleCode> roles,
        Set<String> permissions
) {

    public boolean hasRole(RoleCode role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(RoleCode... requiredRoles) {
        return Arrays.stream(requiredRoles).anyMatch(roles::contains);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}

