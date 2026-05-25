package com.tacticaldistrict.command.personnel.dto;

import com.tacticaldistrict.command.common.attribute.DynamicAttributeValueRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record CreatePersonnelRequest(
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 100) String firstName,
        @Size(max = 100) String middleName,
        @NotBlank @Size(max = 20) String personalNumber,
        @NotNull @Past LocalDate birthDate,
        @NotNull @PastOrPresent LocalDate serviceStart,
        @NotNull Long subdivisionId,
        Long rankId,
        @PastOrPresent LocalDate rankAssignmentDate,
        Set<Long> specialtyIds,
        List<DynamicAttributeValueRequest> rankAttributes
) {
}
