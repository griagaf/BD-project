# Backend Architecture

## Назначение

Backend разделён на HTTP-слой, application services, repository ports, domain policies, infrastructure adapters, mappers и security/access слой. Такое разделение удерживает SQL, бизнес-правила, HTTP-контракты и проверку доступа в разных местах и упрощает сопровождение системы.

## Слои

### Controller

Controller принимает HTTP-запрос, DTO и path/query параметры, вызывает application service и возвращает response DTO. Controller не содержит SQL, не вызывает repository напрямую и не принимает бизнес-решения.

### Application Service

Application service управляет use case:

- открывает транзакционную границу;
- получает `UserContext`;
- вызывает `PermissionService` и access checks;
- вызывает domain policy для предметных правил;
- вызывает repository port для чтения и записи;
- вызывает mapper для DTO.

В application service не размещаются `JdbcTemplate`, `ResultSet` mapping и SQL templates.

### Domain Policy

Domain policy содержит предметные правила, которые не зависят от HTTP и SQL. Пример: `BuildingAssignmentPolicy` проверяет, что подразделение и сооружение относятся к одной военной части, сооружение поддерживает размещение и подразделение не закреплено за другим сооружением.

### Repository

Application layer зависит от портов:

- `QueryExecutionPort`;
- `QueryDefinitionPort`;
- `BuildingRepositoryPort`;
- `AlertRepositoryPort`;
- `DashboardRepositoryPort`.

Infrastructure adapters реализуют эти порты и отвечают за доступ к данным. Сложные SQL-запросы размещаются в custom repository:

- `QueryExecutionRepository`;
- `QuerySqlFactory`;
- `BuildingJdbcRepository`;
- `AlertQueryRepository`;
- `DashboardQueryRepository`;
- специализированные query repositories модулей hierarchy, personnel и reports.

Repository не выполняет permission checks и не содержит UI-решений.

## Текущие чистые вертикальные срезы

- Query Terminal: registry, parameter resolver, SQL factory, execution port, mapper и application service разделены.
- Buildings: persistence вынесен в repository adapter, правила назначения подразделений в domain policy.
- Alerts: SQL-кандидаты вынесены в repository adapter, service формирует alert DTO и применяет access filtering.
- Dashboard: SQL вынесен в repository port/adapter, readiness calculation и problem zone assembly вынесены в domain components.

### Mapper

Mapper преобразует данные в API DTO. Для Query Terminal используется `QueryResultMapper`: он нормализует имена колонок и формирует CSV без смешивания с execution service.

### Security / Access

Проверки ролей, permissions и scope выполняются через:

- `UserContextProvider`;
- `PermissionService`;
- `AccessControlService`;
- `PermissionEvaluator`;
- scope-aware query helpers.

Frontend скрывает недоступные действия только для удобства; backend остаётся обязательным enforcement layer.

## Транзакции

Read use cases помечаются `@Transactional(readOnly = true)`. Write use cases помечаются `@Transactional`. Repository не управляет бизнес-транзакциями самостоятельно.

## Ошибки

`GlobalExceptionHandler` формирует единый ответ ошибки с `code`, `message`, `details`, `fieldErrors`, `validationErrors` и `traceId`. Raw SQL errors, stacktrace и технические constraint names не возвращаются пользователю напрямую.
