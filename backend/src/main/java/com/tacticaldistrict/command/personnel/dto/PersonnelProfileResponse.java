package com.tacticaldistrict.command.personnel.dto;

import com.tacticaldistrict.command.common.dto.AttributeValueResponse;
import java.time.LocalDate;
import java.util.List;

public record PersonnelProfileResponse(
        PersonnelResponse personnel,
        String formationName,
        String assignmentPath,
        LocalDate profileGeneratedAt,
        List<AttributeValueResponse> rankAttributes,
        List<ChainOfCommandNodeResponse> chainOfCommand
) {
}
