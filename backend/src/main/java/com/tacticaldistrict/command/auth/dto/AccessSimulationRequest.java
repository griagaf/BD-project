package com.tacticaldistrict.command.auth.dto;

import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.security.model.ObjectType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AccessSimulationRequest(
        @NotNull RoleCode role,
        ObjectType objectType,
        @Positive Long objectId
) {
}
