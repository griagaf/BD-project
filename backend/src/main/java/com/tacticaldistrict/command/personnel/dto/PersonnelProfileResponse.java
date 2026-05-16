package com.tacticaldistrict.command.personnel.dto;

import java.time.LocalDate;
import java.util.List;

public record PersonnelProfileResponse(
        PersonnelResponse personnel,
        String formationName,
        String assignmentPath,
        LocalDate profileGeneratedAt,
        List<ChainOfCommandNodeResponse> chainOfCommand
) {
}
