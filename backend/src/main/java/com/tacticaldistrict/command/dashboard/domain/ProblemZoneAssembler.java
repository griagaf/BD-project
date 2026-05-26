package com.tacticaldistrict.command.dashboard.domain;

import com.tacticaldistrict.command.alert.dto.TacticalAlertDto;
import com.tacticaldistrict.command.dashboard.dto.ProblemZoneDto;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProblemZoneAssembler {

    public List<ProblemZoneDto> assemble(List<TacticalAlertDto> alerts) {
        return alerts.stream()
                .collect(Collectors.groupingBy(TacticalAlertDto::type, java.util.LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> new ProblemZoneDto(
                        entry.getKey(),
                        label(entry.getKey()),
                        entry.getValue().stream().map(TacticalAlertDto::severity).findFirst().orElse("LOW"),
                        (long) entry.getValue().size()
                ))
                .toList();
    }

    private String label(String type) {
        return switch (type) {
            case "UNIT_WITHOUT_EQUIPMENT" -> "Части без техники";
            case "UNIT_WITHOUT_WEAPONS" -> "Части без вооружения";
            case "BUILDING_WITHOUT_SUBDIVISIONS" -> "Свободные сооружения";
            case "BUILDING_OVERLOADED" -> "Перегруженные сооружения";
            case "SPECIALTY_WITHOUT_SPECIALISTS" -> "Специальности без специалистов";
            case "EQUIPMENT_QUANTITY_EXCEEDED" -> "Превышение количества техники";
            case "WEAPON_QUANTITY_EXCEEDED" -> "Превышение количества вооружения";
            default -> type;
        };
    }
}
