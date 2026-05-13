# TACTICAL DISTRICT COMMAND: implementation plan

## Назначение

Документ описывает пошаговый план реализации приложения **TACTICAL DISTRICT COMMAND**.

План разделен на четыре этапа:

1. MVP.
2. Core version.
3. Visual version.
4. Final defense version.

Главный принцип реализации:

```text
Сначала стабильный вертикальный срез.
Затем расширение функциональности.
Затем визуальный слой.
Затем полировка демонстрационного сценария.
```

## Этап 1. MVP

### Цель

Собрать минимально работающее приложение, которое запускается одной командой и показывает полный путь:

```text
Frontend -> Backend -> PostgreSQL -> SQL query result -> UI
```

### Scope

- `docker-compose`;
- backend skeleton;
- frontend skeleton;
- auth;
- роли;
- dashboard;
- базовые CRUD;
- один-два SQL-запроса через Query Terminal.

### Задачи

#### Infrastructure

1. Создать `docker-compose.yml`.
2. Поднять PostgreSQL container.
3. Добавить backend Dockerfile.
4. Добавить frontend Dockerfile.
5. Добавить `.env.example`.
6. Проверить запуск:

```bash
docker compose up --build
```

#### Database

1. Перенести `create.txt` в Flyway migration.
2. Подключить seed data.
3. Подключить базовые views из `selects/`.
4. Подключить основные triggers.

#### Backend skeleton

1. Создать Spring Boot проект.
2. Настроить профили `dev`, `docker`, `test`.
3. Подключить PostgreSQL.
4. Подключить Flyway.
5. Подключить Spring Security.
6. Подключить Swagger.
7. Добавить `GlobalExceptionHandler`.

#### Auth and roles

1. Создать таблицы:
   - `users`;
   - `roles`;
   - `user_roles`;
   - `command_assignments`;
   - `refresh_tokens`.
2. Реализовать login.
3. Реализовать JWT access token.
4. Реализовать `GET /api/auth/me`.
5. Добавить demo users:
   - `admin.district`;
   - `analyst.staff`;
   - `unit.cmd.1`;
   - `soldier.demo`.

#### Backend API

1. Реализовать dashboard summary:

```http
GET /api/dashboard/summary
```

2. Реализовать CRUD для:
   - personnel;
   - units;
   - subdivisions.

3. Реализовать Query Terminal для 1-2 templates:
   - `FIND_UNITS_IN_FORMATION`;
   - `FIND_UNIT_EQUIPMENT`.

#### Frontend skeleton

1. Создать Vite + React + TypeScript.
2. Подключить Tailwind CSS.
3. Подключить shadcn/ui.
4. Настроить routing.
5. Настроить Zustand auth store.
6. Настроить API client.
7. Настроить TanStack Query.

#### Frontend pages

1. `LoginPage`.
2. `DashboardPage`.
3. `PersonnelPage`.
4. `HierarchyPage` в простом виде.
5. `IntelligenceTerminalPage` с 1-2 запросами.

### Порядок реализации

1. Infrastructure.
2. Database migrations.
3. Backend skeleton.
4. Auth and roles.
5. Basic REST API.
6. Frontend skeleton.
7. Login flow.
8. Dashboard.
9. Basic CRUD.
10. Query Terminal vertical slice.

### Ожидаемый результат

К концу этапа приложение:

- запускается через `docker compose up --build`;
- открывает frontend;
- позволяет войти под demo user;
- показывает dashboard;
- позволяет посмотреть и изменить базовые сущности;
- выполняет 1-2 SQL-запроса через Query Terminal;
- имеет Swagger UI.

### Риски

| Риск | Решение |
|---|---|
| Сложно быстро подключить все triggers | подключить критичные triggers первыми |
| JWT refresh flow занимает время | оставить access token + logout, refresh добавить после |
| CRUD слишком широкий | взять personnel, units, subdivisions |
| Frontend layout долго полировать | сделать простой sidebar + content layout |

### Упрощение при нехватке времени

- использовать только access token без refresh token;
- сделать dashboard из 4-6 счетчиков;
- CRUD сделать только для personnel и units;
- Query Terminal ограничить двумя templates;
- использовать обычный table вместо TanStack Table на первом этапе.

## Этап 2. Core Version

### Цель

Сделать основную функциональность системы: все SQL templates, дерево структуры, поиск, цепочка подчиненности и alert center.

### Scope

- все 13 Query Templates;
- Focus Tree;
- Passport Card;
- Personnel Search;
- Chain of Command;
- Alert Center.

### Задачи

#### Query Terminal

1. Описать все 13 `QueryTemplate`.
2. Для каждого template задать:
   - code;
   - label;
   - description;
   - parameters;
   - required permissions.
3. Реализовать `QueryTemplateService`.
4. Реализовать `QueryExecutorService`.
5. Добавить `NamedParameterJdbcTemplate`.
6. Добавить scope checks для результатов.
7. Добавить CSV export.

#### Focus Tree

1. Реализовать endpoints:

```http
GET /api/focus-tree/roots
GET /api/focus-tree/nodes/{type}/{id}/children
GET /api/focus-tree/focus
GET /api/focus-tree/personnel/{personnelId}/chain
GET /api/focus-tree/nodes/{type}/{id}/passport
```

2. Реализовать lazy-load children.
3. Реализовать scope-фильтрацию дерева.
4. Реализовать frontend store для selected node.
5. Реализовать `Strategic Tree`.
6. Реализовать `Focus Mode`.
7. Реализовать `Chain Mode`.

#### Passport Card

1. Реализовать backend DTO:
   - `NodePassportDto`;
   - `NodeContextDto`.
2. Реализовать центральную карточку объекта.
3. Показать:
   - название;
   - тип;
   - статус;
   - командир;
   - parent;
   - counters;
   - linked resources.

#### Personnel Search

1. Добавить фильтры:
   - name;
   - rank;
   - specialty;
   - unit;
   - subdivision.
2. Добавить scoped search.
3. Добавить переход из результата поиска в:
   - personnel card;
   - Chain Mode.

#### Chain of Command

1. Использовать логику `task4.sql`.
2. Передавать `personnelId` как named parameter.
3. Отображать chain как timeline.
4. Связать каждый node с Passport Card.

#### Alert Center

1. Реализовать генерацию alert'ов:
   - части без техники;
   - части без вооружения;
   - сооружения без подразделений;
   - перегруженные сооружения;
   - специальности без специалистов;
   - превышение количества техники/вооружения.
2. Реализовать severity.
3. Реализовать filters.
4. Реализовать Alert to Action:
   - open object;
   - related data;
   - report;
   - query terminal.

### Порядок реализации

1. Все Query Templates.
2. Scope checks для Query Terminal.
3. Focus Tree API.
4. Focus Tree frontend.
5. Passport Card.
6. Personnel Search.
7. Chain of Command.
8. Alert Center backend.
9. Alert Center frontend.
10. CSV export для Query Terminal.

### Ожидаемый результат

К концу этапа приложение:

- выполняет все 13 SQL-запросов через красивый Query Terminal;
- показывает интерактивную структуру округа;
- позволяет открыть паспорт объекта;
- строит цепочку подчиненности военнослужащего;
- показывает alert'ы по данным БД;
- учитывает роли и command scope.

### Риски

| Риск | Решение |
|---|---|
| Все 13 запросов имеют разные формы результата | возвращать универсальный `columns + rows` |
| Focus Tree может стать сложным | делать cards/list layout без canvas |
| Scope для всех templates занимает время | сначала scope по formation/unit, затем расширить |
| Alert Center может дублировать Query Terminal | использовать те же views и похожие SQL rules |

### Упрощение при нехватке времени

- в Focus Tree оставить Strategic Tree и Focus Mode без виртуализации;
- Chain Mode сделать только для personnel detail page;
- Alert Center сделать read-only без acknowledge/resolve;
- CSV export оставить только для Query Terminal;
- для некоторых Query Templates использовать фиксированный набор параметров.

## Этап 3. Visual Version

### Цель

Сделать приложение визуально сильным: tactical UI, анимации, radar charts, Smart Mission Report и Access Simulation Mode.

### Scope

- tactical UI;
- animations;
- Readiness Radar;
- Smart Mission Report;
- Access Simulation Mode.

### Задачи

#### Tactical UI

1. Привести все страницы к единому layout:
   - Sidebar;
   - Topbar;
   - Content area.
2. Добавить command-style визуальный язык:
   - dark background;
   - compact cards;
   - status badges;
   - severity colors;
   - dense tables.
3. Унифицировать:
   - buttons;
   - forms;
   - tables;
   - dialogs;
   - empty states;
   - loading states.

#### Animations

1. Добавить Framer Motion для:
   - page transitions;
   - tree node expansion;
   - dialog appearance;
   - alert cards;
   - report preview sections.
2. Ограничить анимации короткими переходами.

#### Readiness Radar

1. Реализовать backend readiness calculation.
2. Реализовать endpoints:

```http
GET /api/readiness/radar
GET /api/readiness/summary
```

3. Реализовать Recharts RadarChart.
4. Добавить score breakdown:
   - personnel;
   - equipment;
   - weapons;
   - specialists;
   - infrastructure.

#### Smart Mission Report

1. Реализовать object selector.
2. Реализовать endpoint:

```http
POST /api/reports/smart-mission/generate
```

3. Собрать report sections:
   - object info;
   - commanders;
   - personnel;
   - equipment;
   - weapons;
   - buildings;
   - specialties;
   - alerts;
   - readiness;
   - recommendations.
4. Добавить rule-based recommendations.
5. Добавить report preview.
6. Добавить CSV export.

#### Access Simulation Mode

1. Добавить frontend mode для ADMIN:
   - выбрать роль;
   - увидеть доступные разделы;
   - увидеть скрытые кнопки;
   - посмотреть permission summary.
2. Не использовать simulation как backend authorization.
3. Отображать banner:

```text
Access Simulation: UNIT_COMMANDER
```

### Порядок реализации

1. Tactical layout polish.
2. Shared UI components.
3. Animations.
4. Readiness backend.
5. Readiness frontend.
6. Smart Mission Report backend.
7. Smart Mission Report frontend.
8. Access Simulation Mode.
9. Visual QA for main screens.

### Ожидаемый результат

К концу этапа приложение:

- выглядит как единый tactical command center;
- имеет интерактивные dashboard charts;
- показывает readiness radar;
- генерирует Smart Mission Report;
- позволяет визуально сравнить доступ разных ролей;
- имеет аккуратные loading/error/empty states.

### Риски

| Риск | Решение |
|---|---|
| Визуальная полировка съедает время | сначала оформить 3 ключевых экрана: Dashboard, Focus Tree, Query Terminal |
| Smart Mission Report становится слишком большим | сделать summary sections без глубоких detail таблиц |
| Access Simulation Mode путает реальные права | clearly mark it as UI simulation and keep backend checks unchanged |
| Radar формулы спорные | использовать простые прозрачные формулы |

### Упрощение при нехватке времени

- Smart Mission Report сделать без PDF;
- Access Simulation Mode сделать только как role switcher для sidebar/buttons;
- animations оставить только на page transitions и tree expansion;
- Readiness Radar считать по простым alert penalties;
- report preview сделать без сложной печатной верстки.

## Этап 4. Final Defense Version

### Цель

Стабилизировать проект, подготовить демонстрационный сценарий, seed data, README, Swagger, screenshots и надежный Docker запуск.

### Scope

- demo users;
- сценарий показа;
- seed data;
- README;
- screenshots;
- Swagger;
- стабильный docker запуск.

### Задачи

#### Demo users

Создать пользователей:

```text
admin.district
analyst.staff
army.cmd.1
brigade.cmd.1
unit.cmd.1
company.cmd.1
platoon.cmd.1
soldier.demo
```

Для каждого пользователя:

- роль;
- command assignment;
- связанный `personnel_id`;
- понятный пароль;
- ожидаемый scope.

#### Seed data

Проверить, что seed содержит:

- округ;
- армии;
- формирования;
- части;
- роты;
- взводы;
- отделения;
- военнослужащих;
- командиров;
- технику;
- вооружение;
- сооружения;
- специальности;
- данные для alert'ов;
- данные для readiness score.

#### Demo scenario

Сценарий показа:

1. Запустить:

```bash
docker compose up --build
```

2. Открыть frontend.
3. Войти под `admin.district`.
4. Показать Dashboard.
5. Показать Readiness Radar.
6. Показать Focus Tree.
7. Открыть Unit Passport.
8. Показать Personnel Search.
9. Открыть Chain of Command.
10. Выполнить Query Terminal template.
11. Показать Alert Center.
12. Из alert перейти в Query Terminal или Passport Card.
13. Сгенерировать Smart Mission Report.
14. Показать Access Simulation Mode.
15. Войти под `unit.cmd.1` и показать scope restriction.
16. Войти под `soldier.demo` и показать personal-only access.

#### README

README должен содержать:

- описание проекта;
- стек;
- структуру репозитория;
- ссылки на документацию;
- setup instructions;
- `.env.example`;
- команду запуска;
- demo users;
- URLs;
- troubleshooting.

#### Screenshots

Подготовить screenshots:

```text
docs/screenshots/login.png
docs/screenshots/dashboard.png
docs/screenshots/focus-tree.png
docs/screenshots/query-terminal.png
docs/screenshots/alerts.png
docs/screenshots/smart-report.png
```

README links:

```md
![Dashboard](docs/screenshots/dashboard.png)
```

#### Swagger

Swagger должен иметь:

- title;
- version;
- bearer auth;
- grouped tags;
- request/response examples;
- documented 401/403/404/409 responses.

Tags:

```text
Auth
Users
Personnel
Hierarchy
Units
Equipment
Weapons
Buildings
Specialties
Intelligence
Alerts
Reports
Dashboard
Readiness
Audit
```

#### Stable Docker

Проверки:

```bash
docker compose down -v
docker compose up --build
docker compose ps
docker compose logs -f backend
```

Health URLs:

```text
http://localhost:3000/health
http://localhost:8080/actuator/health
```

### Порядок реализации

1. Проверить seed data.
2. Проверить demo users.
3. Проверить все роли.
4. Проверить Docker reset from zero.
5. Проверить Swagger.
6. Проверить README.
7. Сделать screenshots.
8. Пройти demo scenario.
9. Исправить найденные ошибки.
10. Финальный запуск с чистой БД.

### Ожидаемый результат

К концу этапа проект:

- стабильно запускается одной командой;
- имеет demo users и demo data;
- показывает основные возможности без ручной подготовки БД;
- имеет README и Swagger;
- содержит screenshots;
- демонстрирует роли и scope;
- выглядит production-like.

### Риски

| Риск | Решение |
|---|---|
| Docker запускается нестабильно | healthchecks, depends_on, корректные env variables |
| Seed data не покрывает alert'ы | добавить специальные записи для проблемных зон |
| Demo user не видит нужные данные | проверить command assignments |
| Swagger не отражает auth | добавить bearer scheme |
| UI ломается на пустых данных | добавить empty states |

### Упрощение при нехватке времени

- screenshots сделать только для 4 основных экранов;
- Smart Mission Report экспортировать только CSV;
- оставить PDF endpoint вне основного сценария;
- Access Simulation Mode ограничить sidebar/buttons;
- Swagger examples добавить только для ключевых endpoints;
- demo scenario сократить до admin, analyst, soldier.

## Общий календарный порядок

```text
1. Infrastructure
2. Database migrations
3. Backend auth/security
4. Backend CRUD
5. Frontend skeleton/auth
6. Dashboard
7. Query Terminal vertical slice
8. Full Query Terminal
9. Focus Tree
10. Personnel Search and Chain Mode
11. Alert Center
12. Readiness Radar
13. Smart Mission Report
14. Access Simulation
15. Hardening and documentation
```

## Приоритеты

Если времени мало, приоритет такой:

```text
1. Docker запуск
2. Auth + roles
3. Dashboard
4. CRUD
5. Query Terminal
6. Focus Tree
7. Alerts
8. Readiness
9. Smart Mission Report
10. Visual polish
```

## Definition of Done

Проект считается готовым, когда выполняются условия:

- `docker compose up --build` поднимает все сервисы;
- backend проходит healthcheck;
- frontend открывается в браузере;
- БД создается и наполняется автоматически;
- demo users могут войти;
- роли отличаются по доступу;
- dashboard показывает данные;
- CRUD работает с audit log;
- Query Terminal выполняет templates;
- Focus Tree открывает паспорт объекта;
- Alert Center показывает проблемы;
- Smart Mission Report генерируется;
- README содержит setup и ссылки на документацию.

