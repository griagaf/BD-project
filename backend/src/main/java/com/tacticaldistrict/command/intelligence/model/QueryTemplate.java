package com.tacticaldistrict.command.intelligence.model;

import com.tacticaldistrict.command.intelligence.dto.QueryParameterMetadataDto;
import com.tacticaldistrict.command.intelligence.dto.QueryTemplateMetadataDto;
import java.util.List;
import java.util.Set;

public enum QueryTemplate {
    FIND_UNITS_IN_FORMATION(
            "Find units in formation",
            "Returns military units inside the selected formation tree.",
            "UNITS",
            Set.of("structure:read", "unit:read"),
            List.of(param("formationId", "Formation ID", "number", true, "1")),
            "QUERY: FIND UNITS IN FORMATION 1"
    ),
    FIND_OFFICERS(
            "Find officers",
            "Returns personnel with officer ranks inside selected scope.",
            "PERSONNEL",
            Set.of("personnel:read"),
            List.of(param("rankName", "Rank", "text", false, "Captain")),
            "QUERY: FIND OFFICERS IN FORMATION 1"
    ),
    FIND_ENLISTED_PERSONNEL(
            "Find enlisted personnel",
            "Returns enlisted and sergeant personnel inside selected scope.",
            "PERSONNEL",
            Set.of("personnel:read"),
            List.of(param("rankName", "Rank", "text", false, "Sergeant")),
            "QUERY: FIND ENLISTED PERSONNEL IN UNIT 1"
    ),
    FIND_PERSONNEL_COMMAND_CHAIN(
            "Find personnel command chain",
            "Returns chain of command for selected personnel.",
            "PERSONNEL",
            Set.of("personnel:read", "structure:read"),
            List.of(param("personnelId", "Personnel ID", "number", true, "9")),
            "QUERY: FIND COMMAND CHAIN FOR PERSONNEL 9"
    ),
    FIND_UNIT_LOCATIONS(
            "Find unit locations",
            "Returns units and their locations in selected scope.",
            "UNITS",
            Set.of("unit:read", "structure:read"),
            List.of(),
            "QUERY: FIND UNIT LOCATIONS IN FORMATION 1"
    ),
    FIND_UNIT_EQUIPMENT(
            "Find unit equipment",
            "Returns equipment quantities by unit, category and type.",
            "EQUIPMENT",
            Set.of("equipment:read", "unit:read"),
            List.of(
                    param("equipmentCategory", "Category", "text", false, "Armored vehicles"),
                    param("equipmentType", "Type", "text", false, "BMP-2")
            ),
            "QUERY: FIND EQUIPMENT TYPE BMP-2 IN UNIT 1"
    ),
    FIND_BUILDING_USAGE(
            "Find building usage",
            "Returns buildings with subdivision usage counters.",
            "BUILDINGS",
            Set.of("building:read", "unit:read"),
            List.of(param("usage", "Usage", "enum", false, "ALL", List.of("ALL", "EMPTY", "OVERLOADED"))),
            "QUERY: FIND BUILDING USAGE IN UNIT 1"
    ),
    FIND_EQUIPMENT_AVAILABILITY(
            "Find equipment availability",
            "Checks equipment presence, absence or quantity threshold.",
            "EQUIPMENT",
            Set.of("equipment:read", "unit:read"),
            List.of(
                    param("equipmentType", "Equipment type", "text", true, "BMP-2"),
                    param("condition", "Condition", "enum", true, "WITH_QUANTITY_GT", List.of("WITH_QUANTITY_GT", "WITHOUT")),
                    param("minQuantity", "Min quantity", "number", false, "5")
            ),
            "QUERY: FIND UNITS WITHOUT EQUIPMENT BMP-2 IN ARMY 2"
    ),
    FIND_UNIT_WEAPONS(
            "Find unit weapons",
            "Returns weapon quantities by unit, category and type.",
            "WEAPONS",
            Set.of("weapon:read", "unit:read"),
            List.of(
                    param("weaponCategory", "Category", "text", false, "Small arms"),
                    param("weaponType", "Type", "text", false, "AK-74M")
            ),
            "QUERY: FIND WEAPONS TYPE AK-74M IN UNIT 1"
    ),
    FIND_SPECIALTY_COVERAGE(
            "Find specialty coverage",
            "Returns specialties with enough specialists or no coverage.",
            "SPECIALTIES",
            Set.of("personnel:read", "specialty:read"),
            List.of(
                    param("condition", "Condition", "enum", false, "WITH_SPECIALISTS", List.of("WITH_SPECIALISTS", "WITHOUT_SPECIALISTS")),
                    param("minCount", "Min specialists", "number", false, "1")
            ),
            "QUERY: FIND SPECIALTY COVERAGE IN FORMATION 1"
    ),
    FIND_SPECIALISTS(
            "Find specialists",
            "Returns personnel with selected specialty.",
            "PERSONNEL",
            Set.of("personnel:read", "specialty:read"),
            List.of(param("specialtyName", "Specialty", "text", true, "Signal Operator")),
            "QUERY: FIND SPECIALISTS \"Signal Operator\" IN UNIT 1"
    ),
    FIND_WEAPON_AVAILABILITY(
            "Find weapon availability",
            "Checks weapon presence, absence or quantity threshold.",
            "WEAPONS",
            Set.of("weapon:read", "unit:read"),
            List.of(
                    param("weaponType", "Weapon type", "text", true, "AK-74M"),
                    param("condition", "Condition", "enum", true, "WITH_QUANTITY_GT", List.of("WITH_QUANTITY_GT", "WITHOUT")),
                    param("minQuantity", "Min quantity", "number", false, "10")
            ),
            "QUERY: FIND UNITS WITHOUT WEAPON AK-74M IN ARMY 2"
    ),
    FIND_FORMATION_UNIT_EXTREMES(
            "Find formation unit extremes",
            "Returns formations with maximum or minimum units count.",
            "UNITS",
            Set.of("structure:read", "unit:read"),
            List.of(param("direction", "Direction", "enum", false, "MAX", List.of("MAX", "MIN"))),
            "QUERY: FIND FORMATIONS WITH MAX UNITS"
    );

    private final String label;
    private final String description;
    private final String target;
    private final Set<String> requiredPermissions;
    private final List<QueryParameterMetadataDto> parameters;
    private final String exampleCommand;

    QueryTemplate(
            String label,
            String description,
            String target,
            Set<String> requiredPermissions,
            List<QueryParameterMetadataDto> parameters,
            String exampleCommand
    ) {
        this.label = label;
        this.description = description;
        this.target = target;
        this.requiredPermissions = requiredPermissions;
        this.parameters = parameters;
        this.exampleCommand = exampleCommand;
    }

    public QueryTemplateMetadataDto metadata() {
        return new QueryTemplateMetadataDto(name(), label, description, target, parameters, requiredPermissions, exampleCommand);
    }

    public Set<String> requiredPermissions() {
        return requiredPermissions;
    }

    public List<QueryParameterMetadataDto> parameters() {
        return parameters;
    }

    private static QueryParameterMetadataDto param(String name, String label, String type, boolean required, String placeholder) {
        return param(name, label, type, required, placeholder, List.of());
    }

    private static QueryParameterMetadataDto param(String name, String label, String type, boolean required, String placeholder, List<String> options) {
        return new QueryParameterMetadataDto(name, label, type, required, placeholder, options);
    }
}
