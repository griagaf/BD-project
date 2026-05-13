package com.tacticaldistrict.command.auth.dto;

import com.tacticaldistrict.command.security.model.RoleCode;
import jakarta.validation.constraints.NotNull;

public record AccessSimulationRequest(
        @NotNull RoleCode role
) {
}

