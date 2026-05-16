package com.tacticaldistrict.command.hierarchy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record FormationRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 50) String formationType,
        Long parentId,
        @PastOrPresent LocalDate formationDate,
        @Size(max = 30) String status,
        Long commanderId
) {
}
