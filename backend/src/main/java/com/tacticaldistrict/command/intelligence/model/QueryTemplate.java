package com.tacticaldistrict.command.intelligence.model;

import com.tacticaldistrict.command.intelligence.dto.QueryParameterMetadataDto;
import com.tacticaldistrict.command.intelligence.dto.QueryTemplateMetadataDto;
import java.util.List;
import java.util.Set;

public enum QueryTemplate {
    FIND_UNITS_IN_FORMATION(
            "Найти части в соединении",
            "Возвращает военные части внутри выбранной ветки структуры.",
            "UNITS",
            Set.of("structure:read", "unit:read"),
            List.of(param("formationId", "ID соединения", "number", true, "11")),
            "QUERY: НАЙТИ ЧАСТИ В АРМИИ 11"
    ),
    FIND_OFFICERS(
            "Найти офицеров",
            "Возвращает военнослужащих с офицерскими званиями в выбранной области.",
            "PERSONNEL",
            Set.of("personnel:read"),
            List.of(param("rankName", "Звание", "text", false, "Капитан")),
            "QUERY: НАЙТИ ОФИЦЕРОВ В АРМИИ 11"
    ),
    FIND_ENLISTED_PERSONNEL(
            "Найти сержантов и рядовых",
            "Возвращает сержантский и рядовой состав в выбранной области.",
            "PERSONNEL",
            Set.of("personnel:read"),
            List.of(param("rankName", "Звание", "text", false, "Сержант")),
            "QUERY: НАЙТИ СЕРЖАНТОВ В ЧАСТИ 11101"
    ),
    FIND_PERSONNEL_COMMAND_CHAIN(
            "Найти цепочку командования",
            "Возвращает цепочку подчинённости для выбранного военнослужащего.",
            "PERSONNEL",
            Set.of("personnel:read", "structure:read"),
            List.of(param("personnelId", "ID военнослужащего", "number", true, "1110101111")),
            "QUERY: НАЙТИ ЦЕПОЧКУ КОМАНДОВАНИЯ ДЛЯ 1110101111"
    ),
    FIND_UNIT_LOCATIONS(
            "Найти дислокацию частей",
            "Возвращает военные части и места их дислокации в выбранной области.",
            "UNITS",
            Set.of("unit:read", "structure:read"),
            List.of(),
            "QUERY: НАЙТИ ДИСЛОКАЦИЮ ЧАСТЕЙ В АРМИИ 11"
    ),
    FIND_UNIT_EQUIPMENT(
            "Найти технику частей",
            "Возвращает количество техники по частям, категориям и типам.",
            "EQUIPMENT",
            Set.of("equipment:read", "unit:read"),
            List.of(
                    param("equipmentCategory", "Категория", "text", false, "бронетехника"),
                    param("equipmentType", "Тип", "text", false, "BMP-2")
            ),
            "QUERY: НАЙТИ ТЕХНИКУ BMP-2 В ЧАСТИ 11101"
    ),
    FIND_BUILDING_USAGE(
            "Найти использование сооружений",
            "Возвращает сооружения со счётчиком закреплённых подразделений.",
            "BUILDINGS",
            Set.of("building:read", "unit:read"),
            List.of(param("usage", "Использование", "enum", false, "ALL", List.of("ALL", "EMPTY", "OVERLOADED"))),
            "QUERY: НАЙТИ ИСПОЛЬЗОВАНИЕ СООРУЖЕНИЙ В ЧАСТИ 11101"
    ),
    FIND_EQUIPMENT_AVAILABILITY(
            "Проверить наличие техники",
            "Проверяет наличие, отсутствие или превышение количества техники.",
            "EQUIPMENT",
            Set.of("equipment:read", "unit:read"),
            List.of(
                    param("equipmentType", "Тип техники", "text", true, "BMP-2"),
                    param("condition", "Условие", "enum", true, "WITH_QUANTITY_GT", List.of("WITH_QUANTITY_GT", "WITHOUT")),
                    param("minQuantity", "Минимальное количество", "number", false, "5")
            ),
            "QUERY: НАЙТИ ЧАСТИ БЕЗ ТЕХНИКИ BMP-2 В АРМИИ 11"
    ),
    FIND_UNIT_WEAPONS(
            "Найти вооружение частей",
            "Возвращает количество вооружения по частям, категориям и типам.",
            "WEAPONS",
            Set.of("weapon:read", "unit:read"),
            List.of(
                    param("weaponCategory", "Категория", "text", false, "стрелковое оружие"),
                    param("weaponType", "Тип", "text", false, "AK-74M")
            ),
            "QUERY: НАЙТИ ВООРУЖЕНИЕ AK-74M В ЧАСТИ 11101"
    ),
    FIND_SPECIALTY_COVERAGE(
            "Найти покрытие специальностей",
            "Возвращает специальности с достаточным количеством специалистов или без покрытия.",
            "SPECIALTIES",
            Set.of("personnel:read", "specialty:read"),
            List.of(
                    param("condition", "Условие", "enum", false, "WITH_SPECIALISTS", List.of("WITH_SPECIALISTS", "WITHOUT_SPECIALISTS")),
                    param("minCount", "Минимум специалистов", "number", false, "1")
            ),
            "QUERY: НАЙТИ ПОКРЫТИЕ СПЕЦИАЛЬНОСТЕЙ В АРМИИ 11"
    ),
    FIND_SPECIALISTS(
            "Найти специалистов",
            "Возвращает военнослужащих с выбранной специальностью.",
            "PERSONNEL",
            Set.of("personnel:read", "specialty:read"),
            List.of(param("specialtyName", "Специальность", "text", true, "оператор связи")),
            "QUERY: НАЙТИ СПЕЦИАЛИСТОВ \"оператор связи\" В ЧАСТИ 11101"
    ),
    FIND_WEAPON_AVAILABILITY(
            "Проверить наличие вооружения",
            "Проверяет наличие, отсутствие или превышение количества вооружения.",
            "WEAPONS",
            Set.of("weapon:read", "unit:read"),
            List.of(
                    param("weaponType", "Тип вооружения", "text", true, "AK-74M"),
                    param("condition", "Условие", "enum", true, "WITH_QUANTITY_GT", List.of("WITH_QUANTITY_GT", "WITHOUT")),
                    param("minQuantity", "Минимальное количество", "number", false, "10")
            ),
            "QUERY: НАЙТИ ЧАСТИ БЕЗ ВООРУЖЕНИЯ AK-74M В АРМИИ 11"
    ),
    FIND_FORMATION_UNIT_EXTREMES(
            "Найти соединения по числу частей",
            "Возвращает соединения с максимальным или минимальным количеством частей.",
            "UNITS",
            Set.of("structure:read", "unit:read"),
            List.of(param("direction", "Направление", "enum", false, "MAX", List.of("MAX", "MIN"))),
            "QUERY: НАЙТИ СОЕДИНЕНИЯ С МАКСИМУМОМ ЧАСТЕЙ"
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
