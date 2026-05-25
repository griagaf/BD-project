# Frontend

## Назначение

Frontend предоставляет русскоязычный tactical command interface поверх REST API. Он отвечает за навигацию, формы, таблицы, визуализацию готовности, Query Terminal, паспорта объектов и access-aware UX.

## Структура

- `app` — router, providers, bootstrapping.
- `pages` — страницы маршрутов.
- `features` — доменные компоненты, API clients и типы.
- `shared` — UI primitives, i18n, API client, helpers.

## UX-паттерны

- Таблицы имеют горизонтальный scroll wrapper и пагинацию.
- Большие справочники выбираются через searchable combobox.
- Dropdown рендерится через portal и не обрезается контейнерами.
- Формы используют подсказки, placeholders и человекочитаемые labels.
- Русский язык включён по умолчанию; выбранный язык сохраняется в localStorage.

## Access-aware UI

Frontend скрывает или отключает действия без прав, но не является источником безопасности. После login, logout и смены simulation mode query cache очищается.
