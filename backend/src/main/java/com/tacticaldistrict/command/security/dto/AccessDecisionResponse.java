package com.tacticaldistrict.command.security.dto;

import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.security.model.PermissionAction;

public record AccessDecisionResponse(
        ObjectType objectType,
        Long objectId,
        PermissionAction action,
        boolean allowed
) {
}
