# Backend

## Назначение

Backend предоставляет REST API, применяет правила доступа, выполняет SQL-аналитику, валидирует изменения и возвращает frontend только те данные, которые доступны текущему пользователю.

## Слои

- Controller принимает HTTP-запросы, DTO и validation annotations.
- Service содержит бизнес-логику, permission checks и orchestration.
- Repository/JdbcTemplate выполняет доступ к БД.
- Mapper преобразует Entity/SQL rows в DTO.
- GlobalExceptionHandler формирует человекочитаемые ошибки с `traceId`.

## Доступ к данным

CRUD-операции используют JPA там, где модель проста. Агрегаты, паспорта, дерево, отчёты, lookup-и и Query Terminal используют `NamedParameterJdbcTemplate`, чтобы точно соответствовать SQL-структуре.

## Ошибки

Backend не отдаёт raw SQL, stacktrace и имена constraints как пользовательские сообщения. Ответ ошибки содержит `code`, `message`, `details`, `fieldErrors`, `validationErrors` и `traceId`.

## Производительность

Списки используют pagination и фильтры. Lookup endpoints поддерживают search и limit. User-specific данные не кэшируются общим кэшем, чтобы исключить пересечение областей доступа.
