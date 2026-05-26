# Query Terminal Architecture

## Компоненты

### QueryTemplateRegistry

`QueryTemplateRegistry` хранит список доступных шаблонов, metadata, параметры, permissions и человекочитаемые описания.

### QueryTemplateService

`QueryTemplateService` предоставляет application-facing API для получения списка шаблонов и выбора шаблона по коду.

### QueryParameterResolver

`QueryParameterResolver` валидирует required parameters и формирует безопасную карту параметров запроса.

### QuerySqlFactory

`QuerySqlFactory` находится в repository layer и строит `QueryDefinition`: SQL template и параметры. Здесь размещены SQL-шаблоны Query Terminal и scope predicates.

### QueryExecutionRepository

`QueryExecutionRepository` выполняет SQL через `NamedParameterJdbcTemplate`. Он не знает о ролях, HTTP и frontend.

### QueryExecutorService

`QueryExecutorService` оркестрирует use case:

1. получает текущий `UserContext`;
2. проверяет permissions шаблона;
3. проверяет scope;
4. валидирует параметры;
5. получает `QueryDefinition`;
6. выполняет repository query;
7. применяет row-level access guard;
8. возвращает `QueryResultDto`.

### QueryResultMapper

`QueryResultMapper` нормализует результат выполнения, формирует список колонок и CSV export.

## Scope Filtering

Scope применяется в двух местах:

- SQL predicates в `QuerySqlFactory` ограничивают выборку выбранной областью;
- row-level guard в `QueryExecutorService` повторно проверяет доступ к объектам результата.

Такой подход защищает Query Terminal от случайного расширения выборки при изменении SQL.

## Параметры

Параметры описаны в metadata шаблона. Required параметры блокируют выполнение только тогда, когда они действительно нужны. Scope/root selection не дублируется отдельным обязательным параметром.
