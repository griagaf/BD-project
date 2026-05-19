package com.tacticaldistrict.command.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UserUpdateRequest(
        @NotBlank
        @Size(max = 120)
        String displayName,

        Long personnelId,

        @Size(min = 8, max = 120)
        String password,

        Set<String> roles,

        Boolean active
) {
}
