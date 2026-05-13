# TACTICAL DISTRICT COMMAND: архитектура веб-приложения

## Назначение документа

Документ фиксирует архитектуру production-like приложения **TACTICAL DISTRICT COMMAND** поверх существующей базы данных военного округа.

Текущие материалы БД находятся в корне проекта:

- `create.txt` - основная DDL-схема;
- `military_district_seed.sql` - наполнение данными;
- `selects/` - SQL-запросы и представления;
- `triggers/` - функции, триггеры и ограничения бизнес-логики БД.

Главная идея приложения: не обычный CRUD, а интерактивный командный центр, где существующая БД используется как основа для dashboard, отчетов, query terminal, дерева структуры, alert center и разграничения доступа.

## Технологический стек

Backend:

- Java 21+;
- Spring Boot 4.x;
- Spring Security;
- JWT;
- PostgreSQL;
- REST API;
- Spring Data JPA для CRUD;
- JdbcClient или NamedParameterJdbcTemplate для сложных SQL-запросов и представлений.

Frontend:

- React 19+;
- TypeScript;
- Vite;
- Tailwind CSS;
- shadcn/ui;
- TanStack Query;
- Zustand;
- Recharts;
- Framer Motion.

Infrastructure:

- Docker;
- Docker Compose;
- запуск одной командой:

```bash
docker compose up --build
```

## Текущая модель данных

Существующая БД уже содержит основные предметные сущности:

| Группа | Таблицы |
|---|---|
| География | `locations` |
| Верхняя структура | `military_formations` |
| Военные части | `military_units` |
| Нижняя структура | `subdivisions` |
| Личный состав | `personnel` |
| Звания | `military_ranks`, `personnel_ranks`, `rank_attribute_types`, `rank_type_attributes`, `rank_attribute_values` |
| Специальности | `specialties`, `personnel_specialties` |
| Техника | `equipment_categories`, `equipment_types`, `equipment_attribute_types`, `equipment_category_attributes`, `equipment_type_attribute_values`, `equipment_in_units` |
| Вооружение | `weapon_categories`, `weapon_types`, `weapon_attribute_types`, `weapon_category_attributes`, `weapon_type_attribute_values`, `weapon_in_units` |
| Сооружения | `buildings`, `subdivision_buildings` |

Иерархия округа в текущей БД делится на два уровня:

```text
military_formations
  Округ
    Армия
      Корпус
        Дивизия
          Бригада

military_units
  Военная часть внутри formation

subdivisions
  Батальон
    Рота
      Взвод
        Отделение
```

## Существующие SQL-слои

В папке `selects/` уже есть заготовки для query terminal и аналитических экранов.

Представления:

| Файл | Назначение |
|---|---|
| `formation_closure.sql` | рекурсивное представление иерархии формирований |
| `personnel_full.sql` | расширенная карточка военнослужащего |
| `personnel_specialities.sql` | военнослужащие и их специальности |
| `unit_equipment.sql` | техника по военным частям |
| `unit_weapons.sql` | вооружение по военным частям |
| `buildings_usage.sql` | использование сооружений подразделениями |

Отдельные запросы:

```text
task1.sql
task2.sql
task3.sql
task4.sql
task5.sql
task6.sql
task7.sql
task8.sql
task9.sql
task10.sql
task11.sql
task12.sql
task13.sql
```

Эти 13 запросов нужно использовать как основу для **Query Terminal**. Пользователь не должен вводить произвольный SQL. Он выбирает один из заранее разрешенных запросов, задает параметры, а backend выполняет подготовленный SQL.

## Триггеры и бизнес-логика БД

Папка `triggers/` содержит важную бизнес-логику, которую backend должен учитывать, а не дублировать полностью.

Update/insert triggers:

- `validate_formation_hierarchy.sql` - проверка иерархии формирований;
- `validate_subdivision_hierarchy.sql` - проверка иерархии подразделений;
- `validate_commander.sql` - общая проверка назначений командиров;
- `formation_commander.sql` - правила для командира формирования;
- `unit_commander.sql` - правила для командира военной части;
- `subdivision_commander.sql` - правила для командира подразделения;
- `subdivision_unit_change.sql` - согласованность подразделения и части;
- `validate_rank_date.sql` - проверка даты присвоения звания;
- `validate_rank_attributes.sql` - проверка атрибутов звания;
- `validate_building.sql` - проверка сооружений;
- `building_unit_consistency.sql` - согласованность сооружения и части;
- `command_requirements_table.sql` - таблица требований к командованию.

Delete triggers:

- `prevent_delete_commander.sql` - запрет удаления действующего командира;
- `prevent_delete_rank.sql` - запрет удаления используемого звания;
- `prevent_delete_unit_with_resources.sql` - запрет удаления части с ресурсами;
- `protect_command_requirements.sql` - защита требований к командованию.

Приложение не обходит эту логику: при попытке некорректного изменения backend получает ошибку PostgreSQL и возвращает пользователю понятное сообщение.

## Общая схема приложения

```text
React Frontend
  - pages
  - widgets
  - TanStack Query
  - Zustand auth/session store
        |
        | HTTPS/REST + JWT
        v
Spring Boot Backend
  - controllers
  - services
  - repositories
  - security
  - query registry
        |
        | SQL/JPA/JdbcClient
        v
PostgreSQL
  - tables
  - views
  - functions
  - triggers
```

## Backend-модули

### Auth Module

Отвечает за вход, JWT, текущего пользователя и роли.

Endpoint'ы:

```http
POST /api/auth/login
GET  /api/auth/me
POST /api/auth/logout
```

### Access Control Module

Отвечает за проверку:

- роли пользователя;
- области командования;
- возможности выполнить действие над конкретным объектом.

Подробная модель описана в `docs/02_access_control.md`.

### Structure Module

Работает с иерархией:

- `military_formations`;
- `military_units`;
- `subdivisions`;
- `v_formation_closure`.

Endpoint'ы:

```http
GET /api/structure/tree
GET /api/formations
GET /api/units
GET /api/subdivisions
```

### Personnel Module

Работает с:

- `personnel`;
- `personnel_ranks`;
- `military_ranks`;
- `personnel_specialties`;
- `v_personnel_full`;
- `v_personnel_specialties`.

Endpoint'ы:

```http
GET    /api/personnel
GET    /api/personnel/{id}
POST   /api/personnel
PUT    /api/personnel/{id}
DELETE /api/personnel/{id}
```

### Equipment Module

Работает с:

- `equipment_categories`;
- `equipment_types`;
- `equipment_in_units`;
- `v_unit_equipment`.

Endpoint'ы:

```http
GET /api/equipment
GET /api/equipment/by-unit
PUT /api/units/{unitId}/equipment/{typeId}
```

### Weapons Module

Работает с:

- `weapon_categories`;
- `weapon_types`;
- `weapon_in_units`;
- `v_unit_weapons`.

Endpoint'ы:

```http
GET /api/weapons
GET /api/weapons/by-unit
PUT /api/units/{unitId}/weapons/{typeId}
```

### Buildings Module

Работает с:

- `buildings`;
- `subdivision_buildings`;
- `v_buildings_usage`.

Endpoint'ы:

```http
GET /api/buildings
GET /api/buildings/usage
POST /api/buildings
PUT /api/buildings/{id}
```

### Query Terminal Module

Исполняет только зарегистрированные SQL-запросы из `selects/`.

Endpoint'ы:

```http
GET  /api/queries
POST /api/queries/{code}/execute
```

Пример ответа:

```json
{
  "code": "TASK_01",
  "title": "Военные части выбранного округа",
  "columns": ["unit_id", "unit_name", "parent_formation_name"],
  "rows": [
    {
      "unit_id": 1,
      "unit_name": "101-я военная часть",
      "parent_formation_name": "1-я армия"
    }
  ]
}
```

### Dashboard Module

Собирает агрегаты для главного экрана.

Источники:

- `military_formations`;
- `military_units`;
- `subdivisions`;
- `personnel`;
- `equipment_in_units`;
- `weapon_in_units`;
- `buildings`;
- представления из `selects/`.

Endpoint:

```http
GET /api/dashboard/summary
```

### Alert Center Module

Alert'ы считаются динамически на backend по данным БД:

- часть без командира;
- подразделение без командира;
- часть без техники;
- часть без вооружения;
- сооружение не связано с подразделениями;
- нарушения, пойманные триггерами при изменении данных.

Endpoint'ы:

```http
GET /api/alerts
POST /api/alerts/{id}/acknowledge
```

## Frontend-модули

Рекомендуемая структура:

```text
frontend/src/
  app/
    router.tsx
    providers.tsx
  pages/
    LoginPage.tsx
    DashboardPage.tsx
    StructurePage.tsx
    PersonnelPage.tsx
    EquipmentPage.tsx
    WeaponsPage.tsx
    BuildingsPage.tsx
    QueryTerminalPage.tsx
    AlertsPage.tsx
    ReportsPage.tsx
    AccessSimulationPage.tsx
  widgets/
    TacticalSidebar.tsx
    TopCommandBar.tsx
    ReadinessRadar.tsx
    StructureTree.tsx
    QueryResultTable.tsx
    AlertFeed.tsx
  features/
    auth/
    structure/
    personnel/
    equipment/
    weapons/
    buildings/
    queries/
    alerts/
    access/
  shared/
    api/
    ui/
    types/
    lib/
```

Главные экраны:

- Dashboard;
- Structure Tree;
- Personnel Registry;
- Equipment Registry;
- Weapons Registry;
- Buildings Usage;
- Query Terminal;
- Alert Center;
- Reports;
- Access Simulation.

## REST API: основные данные

### Текущий пользователь

```json
{
  "id": 1,
  "username": "admin.district",
  "displayName": "District System Admin",
  "roles": ["ADMIN_DISTRICT"],
  "assignments": [
    {
      "objectType": "DISTRICT",
      "objectId": 1
    }
  ]
}
```

### Дерево структуры

```json
{
  "id": 1,
  "type": "FORMATION",
  "formationType": "Округ",
  "name": "Западный военный округ",
  "children": [
    {
      "id": 2,
      "type": "FORMATION",
      "formationType": "Армия",
      "name": "1-я армия",
      "children": []
    }
  ]
}
```

### Техника части

```json
{
  "unitId": 10,
  "unitName": "101-я военная часть",
  "formationName": "7-я бригада",
  "equipmentCategory": "Бронетехника",
  "equipmentType": "Танк",
  "quantity": 12
}
```

### Вооружение части

```json
{
  "unitId": 10,
  "unitName": "101-я военная часть",
  "formationName": "7-я бригада",
  "weaponCategory": "Стрелковое оружие",
  "weaponType": "Автомат",
  "quantity": 120
}
```

## Состав реализации

1. Docker Compose: PostgreSQL, backend, frontend.
2. Инициализация БД из `create.txt`, `military_district_seed.sql`, `selects/`, `triggers/`.
3. Login + JWT.
4. Роли и command assignments.
5. Dashboard с агрегатами.
6. Structure Tree на основе `military_formations`, `military_units`, `subdivisions`.
7. CRUD для `personnel`, `military_units`, `subdivisions`, `equipment_in_units`, `weapon_in_units`.
8. Query Terminal для `task1.sql` - `task13.sql`.
9. Read-only views для `v_personnel_full`, `v_unit_equipment`, `v_unit_weapons`, `v_buildings_usage`.
10. Alert Center на вычисляемых правилах.

## Дополнительные системные компоненты

- таблицу `audit_events`;
- таблицу `alerts`;
- WebSocket/SSE для live alerts;
- PDF/Excel reports;
- refresh tokens;
- OpenAPI/Swagger;
- frontend access simulation mode;
- CI/CD;
- тесты backend и frontend.
