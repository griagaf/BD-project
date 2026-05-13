# TACTICAL DISTRICT COMMAND: Intelligence Query Terminal

## Назначение

**Intelligence Query Terminal** предоставляет визуальный query builder поверх 13 аналитических SQL-запросов из каталога `selects/`.

Пользователь не видит технические названия вида `task1.sql` или "Запрос N". Интерфейс работает как командный конструктор:

```text
FIND -> UNITS -> IN -> FORMATION -> Западный военный округ
```

После выбора параметров frontend показывает псевдокоманду:

```text
QUERY: FIND UNITS IN FORMATION "Западный военный округ"
```

Backend получает структурированный запрос, выбирает разрешенный SQL template, подставляет параметры через `NamedParameterJdbcTemplate` и возвращает табличный результат.

## Общий flow

```text
QueryBuilder UI
  -> QueryTemplateSelector
  -> QueryParamsForm
  -> QueryPreviewTerminal
  -> POST /api/intelligence/queries/{code}/execute
  -> IntelligenceController
  -> QueryTemplateService
  -> QueryPermissionService
  -> QueryExecutorService
  -> NamedParameterJdbcTemplate
  -> QueryResultDto
  -> QueryResultTable
```

## Query builder model

Frontend хранит состояние конструктора в единой модели:

```ts
type QueryBuilderState = {
  templateCode: string
  target: string
  scopeType: "FORMATION" | "UNIT" | "SUBDIVISION" | "PERSONNEL" | "GLOBAL"
  scopeId?: number
  filters: Record<string, unknown>
  condition?: "EQUALS" | "GREATER_THAN" | "LESS_THAN" | "WITHOUT" | "TOP" | "BOTTOM"
  value?: unknown
}
```

Backend принимает нормализованный request:

```json
{
  "scope": {
    "type": "FORMATION",
    "id": 1,
    "name": "Западный военный округ"
  },
  "parameters": {
    "equipmentType": "Т-72Б3",
    "minQuantity": 5
  },
  "previewCommand": "QUERY: FIND UNITS WITH EQUIPMENT \"Т-72Б3\" QUANTITY > 5"
}
```

## QueryTemplate enum

```java
public enum QueryTemplate {
    FIND_UNITS_IN_FORMATION,
    FIND_OFFICERS,
    FIND_ENLISTED_PERSONNEL,
    FIND_PERSONNEL_COMMAND_CHAIN,
    FIND_UNIT_LOCATIONS,
    FIND_UNIT_EQUIPMENT,
    FIND_BUILDING_USAGE,
    FIND_EQUIPMENT_AVAILABILITY,
    FIND_UNIT_WEAPONS,
    FIND_SPECIALTY_COVERAGE,
    FIND_SPECIALISTS,
    FIND_WEAPON_AVAILABILITY,
    FIND_FORMATION_UNIT_EXTREMES
}
```

Каждый enum value содержит metadata:

```java
public enum QueryTemplate {

    FIND_UNITS_IN_FORMATION(
            "FIND_UNITS_IN_FORMATION",
            "Find units in formation",
            "Returns military units inside the selected formation tree.",
            "selects/task1.sql",
            Set.of(RoleCode.ADMIN_DISTRICT, RoleCode.STAFF_ANALYST, RoleCode.ARMY_COMMANDER, RoleCode.FORMATION_COMMANDER),
            ScopePolicy.FORMATION_TREE
    );

    private final String code;
    private final String label;
    private final String description;
    private final String sqlFile;
    private final Set<RoleCode> requiredRoles;
    private final ScopePolicy scopePolicy;
}
```

## Template catalog

### 1. `FIND_UNITS_IN_FORMATION`

Source SQL: `selects/task1.sql`

Purpose: найти военные части внутри выбранного формирования.

Metadata:

| Field | Value |
|---|---|
| code | `FIND_UNITS_IN_FORMATION` |
| label | `Find units in formation` |
| description | Возвращает части, родительские формирования и командиров внутри выбранного дерева формирований. |
| required permissions | `structure:read`, `unit:read` |

Parameters:

| Name | Type | Required | Description |
|---|---|---:|---|
| `formationId` | number | yes | Id корневого формирования |
| `formationName` | string | no | Отображаемое имя формирования |

UI scenario:

```text
Что ищем: Units
Где ищем: Formation
Фильтр: Root formation
Условие: Equals
Значение: Западный военный округ
```

Preview:

```text
QUERY: FIND UNITS IN FORMATION "Западный военный округ"
```

Endpoint:

```http
POST /api/intelligence/queries/FIND_UNITS_IN_FORMATION/execute
```

Request:

```json
{
  "scope": {
    "type": "FORMATION",
    "id": 1,
    "name": "Западный военный округ"
  },
  "parameters": {}
}
```

Result:

```json
{
  "code": "FIND_UNITS_IN_FORMATION",
  "columns": ["unit_id", "unit_name", "parent_formation_name", "parent_formation_type", "commander_rank"],
  "rows": [
    {
      "unit_id": 1,
      "unit_name": "1-й танковый полк",
      "parent_formation_name": "1-я армия",
      "parent_formation_type": "Армия",
      "commander_rank": "Полковник"
    }
  ]
}
```

### 2. `FIND_OFFICERS`

Source SQL: `selects/task2.sql`

Purpose: найти офицеров по формированию, званию или военной части.

Metadata:

| Field | Value |
|---|---|
| code | `FIND_OFFICERS` |
| label | `Find officers` |
| description | Возвращает военнослужащих с категорией звания `Офицерский`. |
| required permissions | `personnel:read` |

Parameters:

| Name | Type | Required | Description |
|---|---|---:|---|
| `scopeType` | enum | yes | `FORMATION` или `UNIT` |
| `scopeId` | number | yes | Id формирования или части |
| `rankName` | string | no | Конкретное офицерское звание |

UI scenario:

```text
Что ищем: Personnel
Где ищем: Formation
Фильтр: Rank category
Условие: Equals
Значение: Officers
Дополнительно: Rank equals "Капитан"
```

Preview:

```text
QUERY: FIND OFFICERS RANK "Капитан" IN FORMATION "Западный военный округ"
```

Endpoint:

```http
POST /api/intelligence/queries/FIND_OFFICERS/execute
```

Request:

```json
{
  "scope": {
    "type": "FORMATION",
    "id": 1,
    "name": "Западный военный округ"
  },
  "parameters": {
    "rankName": "Капитан"
  }
}
```

Result:

```json
{
  "code": "FIND_OFFICERS",
  "columns": ["personnel_id", "last_name", "first_name", "rank_name", "unit_name", "subdivision_name"],
  "rows": [
    {
      "personnel_id": 44,
      "last_name": "Иванов",
      "first_name": "Петр",
      "rank_name": "Капитан",
      "unit_name": "1-й танковый полк",
      "subdivision_name": "1-я рота"
    }
  ]
}
```

### 3. `FIND_ENLISTED_PERSONNEL`

Source SQL: `selects/task3.sql`

Purpose: найти сержантский и рядовой состав по формированию, званию или военной части.

Metadata:

| Field | Value |
|---|---|
| code | `FIND_ENLISTED_PERSONNEL` |
| label | `Find enlisted personnel` |
| description | Возвращает военнослужащих с категорией звания `Сержантский и Рядовой`. |
| required permissions | `personnel:read` |

Parameters:

| Name | Type | Required | Description |
|---|---|---:|---|
| `scopeType` | enum | yes | `FORMATION` или `UNIT` |
| `scopeId` | number | yes | Id формирования или части |
| `rankName` | string | no | Конкретное сержантское или рядовое звание |

UI scenario:

```text
Что ищем: Personnel
Где ищем: Unit
Фильтр: Rank category
Условие: Equals
Значение: Enlisted
```

Preview:

```text
QUERY: FIND ENLISTED PERSONNEL IN UNIT "1-й танковый полк"
```

Endpoint:

```http
POST /api/intelligence/queries/FIND_ENLISTED_PERSONNEL/execute
```

Result:

```json
{
  "code": "FIND_ENLISTED_PERSONNEL",
  "columns": ["personnel_id", "last_name", "first_name", "rank_name", "unit_name"],
  "rows": [
    {
      "personnel_id": 57,
      "last_name": "Смирнов",
      "first_name": "Алексей",
      "rank_name": "Сержант",
      "unit_name": "1-й танковый полк"
    }
  ]
}
```

### 4. `FIND_PERSONNEL_COMMAND_CHAIN`

Source SQL: `selects/task4.sql`

Purpose: построить цепочку подчиненности военнослужащего от его подразделения до формирования верхнего уровня.

Metadata:

| Field | Value |
|---|---|
| code | `FIND_PERSONNEL_COMMAND_CHAIN` |
| label | `Find command chain` |
| description | Возвращает военнослужащего, его подразделения, часть и цепочку формирований с командирами. |
| required permissions | `personnel:read`, `structure:read` |

Parameters:

| Name | Type | Required | Description |
|---|---|---:|---|
| `personnelId` | number | yes | Id военнослужащего |

UI scenario:

```text
Что ищем: Command chain
Где ищем: Personnel card
Фильтр: Personnel
Условие: Equals
Значение: Иванов Петр Сергеевич
```

Preview:

```text
QUERY: TRACE COMMAND CHAIN FOR PERSONNEL 44
```

Endpoint:

```http
POST /api/intelligence/queries/FIND_PERSONNEL_COMMAND_CHAIN/execute
```

Result:

```json
{
  "code": "FIND_PERSONNEL_COMMAND_CHAIN",
  "columns": ["sort_group", "level_no", "object_type", "object_name", "extra_info"],
  "rows": [
    {
      "sort_group": 0,
      "level_no": 0,
      "object_type": "Военнослужащий",
      "object_name": "Иванов Петр Сергеевич",
      "extra_info": "Капитан"
    }
  ]
}
```

### 5. `FIND_UNIT_LOCATIONS`

Source SQL: `selects/task5.sql`

Purpose: найти дислокацию частей по формированию или конкретной части.

Metadata:

| Field | Value |
|---|---|
| code | `FIND_UNIT_LOCATIONS` |
| label | `Find unit locations` |
| description | Возвращает город, адрес и название части. |
| required permissions | `unit:read`, `structure:read` |

Parameters:

| Name | Type | Required | Description |
|---|---|---:|---|
| `scopeType` | enum | yes | `FORMATION` или `UNIT` |
| `scopeId` | number | yes | Id формирования или части |

UI scenario:

```text
Что ищем: Locations
Где ищем: Formation
Фильтр: Unit deployment
Условие: In
Значение: Западный военный округ
```

Preview:

```text
QUERY: FIND UNIT LOCATIONS IN FORMATION "Западный военный округ"
```

Endpoint:

```http
POST /api/intelligence/queries/FIND_UNIT_LOCATIONS/execute
```

Result:

```json
{
  "code": "FIND_UNIT_LOCATIONS",
  "columns": ["unit_name", "city", "address"],
  "rows": [
    {
      "unit_name": "1-й танковый полк",
      "city": "Москва",
      "address": "Военный городок 1"
    }
  ]
}
```

### 6. `FIND_UNIT_EQUIPMENT`

Source SQL: `selects/task6.sql`

Purpose: найти технику частей по формированию, категории, типу или конкретной части.

Metadata:

| Field | Value |
|---|---|
| code | `FIND_UNIT_EQUIPMENT` |
| label | `Find unit equipment` |
| description | Возвращает типы техники и количество по частям. |
| required permissions | `equipment:read`, `unit:read` |

Parameters:

| Name | Type | Required | Description |
|---|---|---:|---|
| `scopeType` | enum | yes | `FORMATION` или `UNIT` |
| `scopeId` | number | yes | Id формирования или части |
| `equipmentCategory` | string | no | Категория техники |
| `equipmentType` | string | no | Тип техники |

UI scenario:

```text
Что ищем: Equipment
Где ищем: Formation
Фильтр: Equipment type
Условие: Equals
Значение: Т-72Б3
```

Preview:

```text
QUERY: FIND EQUIPMENT "Т-72Б3" IN FORMATION "Западный военный округ"
```

Endpoint:

```http
POST /api/intelligence/queries/FIND_UNIT_EQUIPMENT/execute
```

Result:

```json
{
  "code": "FIND_UNIT_EQUIPMENT",
  "columns": ["unit_name", "equipment_category", "equipment_type", "quantity"],
  "rows": [
    {
      "unit_name": "1-й танковый полк",
      "equipment_category": "Основные боевые танки",
      "equipment_type": "Т-72Б3",
      "quantity": 31
    }
  ]
}
```

### 7. `FIND_BUILDING_USAGE`

Source SQL: `selects/task7.sql`

Purpose: анализ использования сооружений.

Metadata:

| Field | Value |
|---|---|
| code | `FIND_BUILDING_USAGE` |
| label | `Find building usage` |
| description | Возвращает сооружения части, сооружения с несколькими подразделениями или неиспользуемые сооружения. |
| required permissions | `building:read`, `unit:read` |

Parameters:

| Name | Type | Required | Description |
|---|---|---:|---|
| `mode` | enum | yes | `BY_UNIT`, `SHARED`, `UNUSED` |
| `unitId` | number | conditional | Обязателен для `BY_UNIT` |

UI scenario:

```text
Что ищем: Buildings
Где ищем: All units
Фильтр: Usage
Условие: Equals
Значение: Unused
```

Preview:

```text
QUERY: FIND UNUSED BUILDINGS
```

Endpoint:

```http
POST /api/intelligence/queries/FIND_BUILDING_USAGE/execute
```

Result:

```json
{
  "code": "FIND_BUILDING_USAGE",
  "columns": ["building_id", "building_name", "unit_name", "subdivisions_count"],
  "rows": [
    {
      "building_id": 5,
      "building_name": "Склад N1",
      "unit_name": "1-й танковый полк",
      "subdivisions_count": 0
    }
  ]
}
```

### 8. `FIND_EQUIPMENT_AVAILABILITY`

Source SQL: `selects/task8.sql`

Purpose: найти части, где есть техника нужного типа в количестве больше порога, или части без техники этого типа.

Metadata:

| Field | Value |
|---|---|
| code | `FIND_EQUIPMENT_AVAILABILITY` |
| label | `Find equipment availability` |
| description | Проверяет наличие или отсутствие конкретного типа техники. |
| required permissions | `equipment:read`, `unit:read` |

Parameters:

| Name | Type | Required | Description |
|---|---|---:|---|
| `equipmentType` | string | yes | Тип техники |
| `condition` | enum | yes | `QUANTITY_GREATER_THAN` или `WITHOUT` |
| `minQuantity` | number | conditional | Порог для `QUANTITY_GREATER_THAN` |
| `scopeType` | enum | no | `FORMATION` или `GLOBAL` |
| `scopeId` | number | no | Id формирования |

UI scenario:

```text
Что ищем: Units
Где ищем: Army
Фильтр: Equipment
Условие: Without
Значение: БМП
```

Preview:

```text
QUERY: FIND UNITS WITHOUT EQUIPMENT "БМП" IN ARMY 2
```

Endpoint:

```http
POST /api/intelligence/queries/FIND_EQUIPMENT_AVAILABILITY/execute
```

Result:

```json
{
  "code": "FIND_EQUIPMENT_AVAILABILITY",
  "columns": ["unit_name"],
  "rows": [
    {
      "unit_name": "3-я ремонтная часть"
    }
  ]
}
```

### 9. `FIND_UNIT_WEAPONS`

Source SQL: `selects/task9.sql`

Purpose: найти вооружение частей по формированию, категории, типу или конкретной части.

Metadata:

| Field | Value |
|---|---|
| code | `FIND_UNIT_WEAPONS` |
| label | `Find unit weapons` |
| description | Возвращает типы вооружения и количество по частям. |
| required permissions | `weapon:read`, `unit:read` |

Parameters:

| Name | Type | Required | Description |
|---|---|---:|---|
| `scopeType` | enum | yes | `FORMATION` или `UNIT` |
| `scopeId` | number | yes | Id формирования или части |
| `weaponCategory` | string | no | Категория вооружения |
| `weaponType` | string | no | Тип вооружения |

UI scenario:

```text
Что ищем: Weapons
Где ищем: Formation
Фильтр: Weapon category
Условие: Equals
Значение: Автоматическое оружие
```

Preview:

```text
QUERY: FIND WEAPONS CATEGORY "Автоматическое оружие" IN FORMATION "Западный военный округ"
```

Endpoint:

```http
POST /api/intelligence/queries/FIND_UNIT_WEAPONS/execute
```

Result:

```json
{
  "code": "FIND_UNIT_WEAPONS",
  "columns": ["unit_name", "weapon_category", "weapon_type", "quantity"],
  "rows": [
    {
      "unit_name": "1-й танковый полк",
      "weapon_category": "Автоматическое оружие",
      "weapon_type": "АК-74М",
      "quantity": 340
    }
  ]
}
```

### 10. `FIND_SPECIALTY_COVERAGE`

Source SQL: `selects/task10.sql`

Purpose: анализ покрытия специальностей по формированию или военной части.

Metadata:

| Field | Value |
|---|---|
| code | `FIND_SPECIALTY_COVERAGE` |
| label | `Find specialty coverage` |
| description | Возвращает специальности с количеством специалистов выше порога или отсутствующие специальности. |
| required permissions | `personnel:read`, `specialty:read` |

Parameters:

| Name | Type | Required | Description |
|---|---|---:|---|
| `scopeType` | enum | yes | `FORMATION` или `UNIT` |
| `scopeId` | number | yes | Id формирования или части |
| `mode` | enum | yes | `ABOVE_THRESHOLD` или `MISSING` |
| `minCount` | number | conditional | Порог для `ABOVE_THRESHOLD` |

UI scenario:

```text
Что ищем: Specialties
Где ищем: Unit
Фильтр: Specialists count
Условие: Greater than
Значение: 5
```

Preview:

```text
QUERY: FIND SPECIALTIES WITH SPECIALISTS > 5 IN UNIT "1-й танковый полк"
```

Endpoint:

```http
POST /api/intelligence/queries/FIND_SPECIALTY_COVERAGE/execute
```

Result:

```json
{
  "code": "FIND_SPECIALTY_COVERAGE",
  "columns": ["specialty_name", "specialists_count"],
  "rows": [
    {
      "specialty_name": "Связист",
      "specialists_count": 8
    }
  ]
}
```

### 11. `FIND_SPECIALISTS`

Source SQL: `selects/task11.sql`

Purpose: найти военнослужащих с выбранной специальностью по формированию, части или подразделению.

Metadata:

| Field | Value |
|---|---|
| code | `FIND_SPECIALISTS` |
| label | `Find specialists` |
| description | Возвращает военнослужащих с выбранной специальностью и их подразделения. |
| required permissions | `personnel:read`, `specialty:read` |

Parameters:

| Name | Type | Required | Description |
|---|---|---:|---|
| `scopeType` | enum | yes | `FORMATION`, `UNIT` или `SUBDIVISION` |
| `scopeId` | number | yes | Id области поиска |
| `specialtyName` | string | yes | Название специальности |

UI scenario:

```text
Что ищем: Personnel
Где ищем: Company
Фильтр: Specialty
Условие: Equals
Значение: Связист
```

Preview:

```text
QUERY: FIND SPECIALISTS "Связист" IN COMPANY "1-я рота"
```

Endpoint:

```http
POST /api/intelligence/queries/FIND_SPECIALISTS/execute
```

Result:

```json
{
  "code": "FIND_SPECIALISTS",
  "columns": ["last_name", "first_name", "rank_name", "specialty_name", "unit_name", "subdivision_name"],
  "rows": [
    {
      "last_name": "Петров",
      "first_name": "Илья",
      "rank_name": "Сержант",
      "specialty_name": "Связист",
      "unit_name": "1-й танковый полк",
      "subdivision_name": "1-я рота"
    }
  ]
}
```

### 12. `FIND_WEAPON_AVAILABILITY`

Source SQL: `selects/task12.sql`

Purpose: найти части, где есть вооружение нужного типа в количестве больше порога, или части без вооружения этого типа.

Metadata:

| Field | Value |
|---|---|
| code | `FIND_WEAPON_AVAILABILITY` |
| label | `Find weapon availability` |
| description | Проверяет наличие или отсутствие конкретного типа вооружения. |
| required permissions | `weapon:read`, `unit:read` |

Parameters:

| Name | Type | Required | Description |
|---|---|---:|---|
| `weaponType` | string | yes | Тип вооружения |
| `condition` | enum | yes | `QUANTITY_GREATER_THAN` или `WITHOUT` |
| `minQuantity` | number | conditional | Порог для `QUANTITY_GREATER_THAN` |
| `scopeType` | enum | no | `FORMATION` или `GLOBAL` |
| `scopeId` | number | no | Id формирования |

UI scenario:

```text
Что ищем: Units
Где ищем: District
Фильтр: Weapon
Условие: Without
Значение: АК-74М
```

Preview:

```text
QUERY: FIND UNITS WITHOUT WEAPON "АК-74М"
```

Endpoint:

```http
POST /api/intelligence/queries/FIND_WEAPON_AVAILABILITY/execute
```

Result:

```json
{
  "code": "FIND_WEAPON_AVAILABILITY",
  "columns": ["unit_name"],
  "rows": [
    {
      "unit_name": "2-я инженерная часть"
    }
  ]
}
```

### 13. `FIND_FORMATION_UNIT_EXTREMES`

Source SQL: `selects/task13.sql`

Purpose: найти формирование с максимальным или минимальным количеством военных частей.

Metadata:

| Field | Value |
|---|---|
| code | `FIND_FORMATION_UNIT_EXTREMES` |
| label | `Find formation unit extremes` |
| description | Возвращает армии, корпуса или дивизии с максимальным или минимальным количеством частей. |
| required permissions | `structure:read`, `unit:read` |

Parameters:

| Name | Type | Required | Description |
|---|---|---:|---|
| `mode` | enum | yes | `MAX_UNITS` или `MIN_UNITS` |
| `formationTypes` | array | no | Типы формирований: `Армия`, `Корпус`, `Дивизия` |

UI scenario:

```text
Что ищем: Formations
Где ищем: Global
Фильтр: Units count
Условие: Top
Значение: Maximum
```

Preview:

```text
QUERY: FIND FORMATIONS WITH MAX UNIT COUNT
```

Endpoint:

```http
POST /api/intelligence/queries/FIND_FORMATION_UNIT_EXTREMES/execute
```

Result:

```json
{
  "code": "FIND_FORMATION_UNIT_EXTREMES",
  "columns": ["formation_type", "formation_name", "units_count"],
  "rows": [
    {
      "formation_type": "Армия",
      "formation_name": "1-я армия",
      "units_count": 12
    }
  ]
}
```

## Backend architecture

### Package structure

```text
com.tacticaldistrict.command.intelligence/
  controller/
    IntelligenceController.java
  dto/
    QueryTemplateDto.java
    QueryParameterDto.java
    QueryExecutionRequest.java
    QueryScopeDto.java
    QueryResultDto.java
    QueryExportRequest.java
  model/
    QueryTemplate.java
    QueryParameterType.java
    QueryCondition.java
    ScopePolicy.java
  service/
    QueryTemplateService.java
    QueryExecutorService.java
    QueryPermissionService.java
    QueryPreviewService.java
    QueryExportService.java
  repository/
    IntelligenceQueryRepository.java
```

### IntelligenceController

```java
@RestController
@RequestMapping("/api/intelligence")
@Tag(name = "Intelligence Query Terminal")
public class IntelligenceController {

    private final QueryTemplateService queryTemplateService;
    private final QueryExecutorService queryExecutorService;
    private final QueryExportService queryExportService;

    @GetMapping("/queries")
    public List<QueryTemplateDto> templates() {
        return queryTemplateService.findAvailableTemplates();
    }

    @GetMapping("/queries/{code}")
    public QueryTemplateDto template(@PathVariable String code) {
        return queryTemplateService.getTemplate(code);
    }

    @PostMapping("/queries/{code}/execute")
    public QueryResultDto execute(
            @PathVariable String code,
            @Valid @RequestBody QueryExecutionRequest request
    ) {
        return queryExecutorService.execute(code, request);
    }

    @PostMapping("/queries/{code}/preview")
    public QueryPreviewDto preview(
            @PathVariable String code,
            @Valid @RequestBody QueryExecutionRequest request
    ) {
        return queryTemplateService.preview(code, request);
    }

    @PostMapping("/queries/{code}/export.csv")
    public ResponseEntity<byte[]> exportCsv(
            @PathVariable String code,
            @Valid @RequestBody QueryExecutionRequest request
    ) {
        CsvExport export = queryExportService.exportCsv(code, request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.fileName() + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(export.content());
    }
}
```

### QueryTemplateService

```java
public interface QueryTemplateService {

    List<QueryTemplateDto> findAvailableTemplates();

    QueryTemplateDto getTemplate(String code);

    QueryTemplate getRequiredTemplate(String code);

    QueryPreviewDto preview(String code, QueryExecutionRequest request);
}
```

Responsibilities:

- хранит список разрешенных templates;
- возвращает metadata для frontend;
- описывает параметры и допустимые условия;
- не выполняет SQL.

### QueryExecutorService

```java
@Service
public class QueryExecutorService {

    private final QueryTemplateService queryTemplateService;
    private final QueryPermissionService queryPermissionService;
    private final IntelligenceQueryRepository intelligenceQueryRepository;
    private final AuditService auditService;
    private final UserContextProvider userContextProvider;

    public QueryResultDto execute(String code, QueryExecutionRequest request) {
        UserContext user = userContextProvider.current();
        QueryTemplate template = queryTemplateService.getRequiredTemplate(code);

        queryPermissionService.checkCanExecute(user, template, request);

        QueryResultDto result = intelligenceQueryRepository.execute(template, request);
        QueryResultDto scopedResult = queryPermissionService.filterResult(user, template, result);

        auditService.queryExecuted(user, template.code());

        return scopedResult;
    }
}
```

### IntelligenceQueryRepository

```java
@Repository
public class IntelligenceQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final QuerySqlRenderer sqlRenderer;

    public QueryResultDto execute(QueryTemplate template, QueryExecutionRequest request) {
        RenderedQuery renderedQuery = sqlRenderer.render(template, request);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                renderedQuery.sql(),
                renderedQuery.parameters()
        );

        List<String> columns = rows.isEmpty()
                ? renderedQuery.columns()
                : List.copyOf(rows.get(0).keySet());

        return new QueryResultDto(
                template.code(),
                template.label(),
                request.previewCommand(),
                columns,
                rows,
                rows.size()
        );
    }
}
```

### QueryResultDto

```java
public record QueryResultDto(
        String code,
        String label,
        String previewCommand,
        List<String> columns,
        List<Map<String, Object>> rows,
        int rowCount
) {
}
```

Request DTO:

```java
public record QueryExecutionRequest(
        QueryScopeDto scope,
        Map<String, Object> parameters,
        String previewCommand
) {
}
```

```java
public record QueryScopeDto(
        String type,
        Long id,
        String name
) {
}
```

Template DTO:

```java
public record QueryTemplateDto(
        String code,
        String label,
        String description,
        List<QueryParameterDto> parameters,
        List<String> requiredPermissions,
        String exampleCommand
) {
}
```

## SQL rendering

SQL templates не конкатенируют пользовательский ввод. Все значения передаются как named parameters.

Example:

```sql
SELECT
    vue.unit_name,
    vue.equipment_category,
    vue.equipment_type,
    vue.quantity
FROM v_unit_equipment vue
JOIN v_formation_closure fc
    ON fc.descendant_formation_id = vue.formation_id
WHERE fc.root_formation_id = :formationId
  AND (:equipmentCategory IS NULL OR vue.equipment_category = :equipmentCategory)
  AND (:equipmentType IS NULL OR vue.equipment_type = :equipmentType)
ORDER BY vue.unit_name, vue.equipment_category, vue.equipment_type;
```

Rendered parameters:

```java
Map.of(
    "formationId", 1L,
    "equipmentCategory", null,
    "equipmentType", "Т-72Б3"
)
```

## Access control for results

Проверка доступа выполняется до SQL и после SQL.

### Before execution

`QueryPermissionService` проверяет:

1. Пользователь аутентифицирован.
2. Роль пользователя входит в `requiredRoles`.
3. Пользователь имеет требуемые permissions.
4. `scope` запроса находится внутри `command_assignments`.
5. Параметры соответствуют allowed schema.

```java
public void checkCanExecute(UserContext user, QueryTemplate template, QueryExecutionRequest request) {
    if (!permissionMatrix.isAllowed(user.roles(), PermissionAction.EXECUTE_QUERY, ObjectType.QUERY)) {
        throw new AccessDeniedException("Query execution is not allowed");
    }

    if (!template.requiredRoles().stream().anyMatch(user.roles()::contains)) {
        throw new AccessDeniedException("Query template is not allowed");
    }

    if (!accessControlService.isInScope(user, request.scope().type(), request.scope().id())) {
        throw new AccessDeniedException("Query scope is outside command assignment");
    }
}
```

### During SQL execution

SQL добавляет scope condition:

```sql
AND fc.root_formation_id IN (:allowedFormationIds)
```

или:

```sql
AND vue.unit_id IN (:allowedUnitIds)
```

Для `ADMIN_DISTRICT` список scope conditions не ограничивает результат. Для командиров и солдат scope conditions обязательны.

### After execution

`filterResult` применяется как дополнительный защитный слой для templates, которые возвращают mixed rows:

```java
public QueryResultDto filterResult(UserContext user, QueryTemplate template, QueryResultDto result) {
    if (user.hasRole(RoleCode.ADMIN_DISTRICT)) {
        return result;
    }

    List<Map<String, Object>> rows = result.rows().stream()
            .filter(row -> rowBelongsToScope(user, template, row))
            .toList();

    return result.withRows(rows);
}
```

Основное правило: backend не возвращает строки вне scope пользователя. Frontend не является источником безопасности.

## CSV export

Export использует тот же execution path, что и обычный запуск запроса:

```text
POST /api/intelligence/queries/{code}/export.csv
  -> checkCanExecute
  -> execute SQL with scope
  -> filterResult
  -> write CSV
```

CSV response:

```http
HTTP/1.1 200 OK
Content-Type: text/csv; charset=UTF-8
Content-Disposition: attachment; filename="FIND_UNIT_EQUIPMENT_2026-05-13.csv"
```

CSV generation:

```java
@Service
public class QueryExportService {

    private final QueryExecutorService queryExecutorService;

    public CsvExport exportCsv(String code, QueryExecutionRequest request) {
        QueryResultDto result = queryExecutorService.execute(code, request);

        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", result.columns())).append("\n");

        for (Map<String, Object> row : result.rows()) {
            String line = result.columns().stream()
                    .map(column -> csvEscape(row.get(column)))
                    .collect(Collectors.joining(","));
            csv.append(line).append("\n");
        }

        String fileName = code + "_" + LocalDate.now() + ".csv";
        return new CsvExport(fileName, csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String csvEscape(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString().replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}
```

CSV export audit action:

```text
EXPORT_QUERY_RESULT
```

## Frontend components

### `QueryBuilder`

Container-компонент для всего сценария:

- хранит выбранный template;
- хранит параметры;
- вызывает execute/export;
- передает preview command в terminal block.

### `QueryTemplateSelector`

Визуальный selector templates:

- сгруппирован по категориям: Structure, Personnel, Equipment, Weapons, Buildings, Specialties;
- показывает label и краткое описание;
- скрывает templates, недоступные текущей роли.

### `QueryParamsForm`

Динамическая форма параметров:

- select для scope;
- combobox для формирования, части, подразделения, военнослужащего;
- combobox для техники, вооружения, специальности, звания;
- segmented control для условий: equals, greater than, without, top, bottom;
- numeric input для threshold.

### `QueryPreviewTerminal`

Псевдотерминал:

```text
QUERY: FIND UNITS WITHOUT EQUIPMENT "БМП" IN ARMY 2
STATUS: READY
SCOPE: ARMY / 2
```

Визуально:

- темная terminal-панель;
- моноширинный шрифт;
- подсветка `QUERY`, target, condition и value;
- copy button;
- execute button рядом, но не внутри terminal output.

### `QueryResultTable`

Таблица результатов:

- sticky header;
- column visibility;
- client-side search по текущему результату;
- row count;
- empty state;
- CSV export button;
- compact density mode.

### Supporting components

```text
QueryScopePicker
QueryConditionSelect
QueryValueInput
QueryHistoryPanel
QueryExportButton
QueryEmptyState
```

## Frontend visual layout

```text
┌─────────────────────────────────────────────────────────────┐
│ Intelligence Query Terminal                                │
├───────────────────────┬─────────────────────────────────────┤
│ Template Selector     │ Query Builder                       │
│ - Units               │ What: Equipment                     │
│ - Personnel           │ Where: Army 2                       │
│ - Equipment           │ Filter: Equipment Type              │
│ - Weapons             │ Condition: Without                  │
│ - Buildings           │ Value: BMP                          │
├───────────────────────┴─────────────────────────────────────┤
│ QUERY: FIND UNITS WITHOUT EQUIPMENT "BMP" IN ARMY 2          │
├─────────────────────────────────────────────────────────────┤
│ Result Table                                                 │
└─────────────────────────────────────────────────────────────┘
```

## REST API summary

```http
GET  /api/intelligence/queries
GET  /api/intelligence/queries/{code}
POST /api/intelligence/queries/{code}/preview
POST /api/intelligence/queries/{code}/execute
POST /api/intelligence/queries/{code}/export.csv
GET  /api/intelligence/dictionaries/formations
GET  /api/intelligence/dictionaries/units
GET  /api/intelligence/dictionaries/subdivisions
GET  /api/intelligence/dictionaries/personnel
GET  /api/intelligence/dictionaries/equipment-types
GET  /api/intelligence/dictionaries/weapon-types
GET  /api/intelligence/dictionaries/specialties
GET  /api/intelligence/dictionaries/ranks
```

Dictionary endpoints return scoped options for the current user.

## Technical constraints

- Пользователь не отправляет raw SQL.
- SQL template выбирается только по known `QueryTemplate`.
- Все параметры передаются через named parameters.
- Все dictionary options фильтруются по scope.
- Result rows фильтруются по scope на backend.
- CSV export использует тот же security path, что и execute.
- Frontend отображает недоступные действия как disabled или hidden, но backend остается единственным enforcement layer.
