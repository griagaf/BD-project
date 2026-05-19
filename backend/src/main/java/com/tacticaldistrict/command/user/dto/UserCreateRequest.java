package com.tacticaldistrict.command.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UserCreateRequest(
        @NotBlank
        @Size(max = 64)
        String username,

        @NotBlank
        @Size(max = 120)
        String displayName,

        Long personnelId,

        @NotBlank
        @Size(min = 8, max = 120)
        String password,

        Set<String> roles,

        Boolean active
) {
}
