# TACTICAL DISTRICT COMMAND: роли и разграничение доступа

## Назначение документа

Документ описывает модель авторизации для приложения **TACTICAL DISTRICT COMMAND** с учетом текущей схемы БД из `create.txt`.

Ключевой принцип:

```text
Права = роль + область командования
```

Роль определяет, **какие действия** пользователь может выполнять.

Command assignment определяет, **где именно** он может их выполнять.

Звание военнослужащего не дает права доступа напрямую. Звание хранится в `military_ranks` и `personnel_ranks`, показывается в интерфейсе и используется предметной логикой, но не является источником полномочий.

## Текущие таблицы предметной области

Модель доступа должна быть связана с существующими таблицами:

| Смысл | Таблица |
|---|---|
| Верхняя военная структура | `military_formations` |
| Военные части | `military_units` |
| Роты, взводы, отделения и батальоны | `subdivisions` |
| Военнослужащие | `personnel` |
| Звания | `military_ranks`, `personnel_ranks` |
| Техника | `equipment_in_units`, `equipment_types` |
| Вооружение | `weapon_in_units`, `weapon_types` |
| Сооружения | `buildings`, `subdivision_buildings` |

## Новые таблицы авторизации

### `users`

Пользователь приложения. Может быть связан с военнослужащим из таблицы `personnel`, но это не обязательно.

```sql
CREATE TABLE users (
    user_id         SERIAL PRIMARY KEY,
    username        VARCHAR(64) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(120) NOT NULL,
    personnel_id    INTEGER NULL REFERENCES personnel(personnel_id) ON DELETE SET NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    CHECK (username <> ''),
    CHECK (display_name <> '')
);
```

Почему `personnel_id` nullable:

- системный администратор может не быть военнослужащим;
- аналитик может быть технической учетной записью;
- командиры и солдаты обычно должны быть связаны с `personnel`.

### `roles`

Справочник ролей.

```sql
CREATE TABLE roles (
    role_id     SERIAL PRIMARY KEY,
    code        VARCHAR(64) NOT NULL UNIQUE,
    name        VARCHAR(120) NOT NULL,
    description TEXT,
    CHECK (code <> ''),
    CHECK (name <> '')
);
```

Роли:

```text
ADMIN_DISTRICT
STAFF_ANALYST
ARMY_COMMANDER
FORMATION_COMMANDER
UNIT_COMMANDER
COMPANY_COMMANDER
PLATOON_COMMANDER
SQUAD_COMMANDER
SOLDIER
```

### `user_roles`

Связь many-to-many между пользователями и ролями.

```sql
CREATE TABLE user_roles (
    user_id     INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    role_id     INTEGER NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);
```

## Command assignments

Таблица `command_assignments` задает область действия военнослужащего, связанного с пользователем через `users.personnel_id`.

```sql
CREATE TABLE command_assignments (
    assignment_id SERIAL PRIMARY KEY,
    soldier_id    INTEGER NOT NULL REFERENCES personnel(personnel_id) ON DELETE CASCADE,
    object_type   VARCHAR(32) NOT NULL,
    object_id     INTEGER NOT NULL,
    starts_at     DATE NOT NULL DEFAULT CURRENT_DATE,
    ends_at       DATE NULL,
    is_primary    BOOLEAN NOT NULL DEFAULT FALSE,
    CHECK (object_type IN (
        'DISTRICT',
        'FORMATION',
        'ARMY',
        'CORPS',
        'DIVISION',
        'BRIGADE',
        'MILITARY_UNIT',
        'BATTALION',
        'COMPANY',
        'PLATOON',
        'SQUAD',
        'SELF'
    )),
    CHECK (ends_at IS NULL OR ends_at >= starts_at)
);
```

`object_id` является полиморфной ссылкой. Его смысл зависит от `object_type`.

| object_type | object_id указывает на |
|---|---|
| `DISTRICT` | `military_formations.formation_id` с `formation_type = 'Округ'` |
| `FORMATION` | `military_formations.formation_id` |
| `ARMY` | `military_formations.formation_id` с `formation_type = 'Армия'` |
| `CORPS` | `military_formations.formation_id` с `formation_type = 'Корпус'` |
| `DIVISION` | `military_formations.formation_id` с `formation_type = 'Дивизия'` |
| `BRIGADE` | `military_formations.formation_id` с `formation_type = 'Бригада'` |
| `MILITARY_UNIT` | `military_units.unit_id` |
| `BATTALION` | `subdivisions.subdivision_id` с `type = 'Батальон'` |
| `COMPANY` | `subdivisions.subdivision_id` с `type = 'Рота'` |
| `PLATOON` | `subdivisions.subdivision_id` с `type = 'Взвод'` |
| `SQUAD` | `subdivisions.subdivision_id` с `type = 'Отделение'` |
| `SELF` | `personnel.personnel_id` |

Модель использует существующую структуру таблиц без введения единой таблицы узлов иерархии.

## Базовые роли

### `ADMIN_DISTRICT`

Полный доступ ко всем данным и настройкам.

Может:

- управлять пользователями;
- назначать роли;
- назначать command assignments;
- выполнять все CRUD-операции;
- запускать все запросы query terminal;
- видеть весь округ.

### `STAFF_ANALYST`

Аналитический read-only доступ.

Может:

- читать структуру округа;
- читать личный состав;
- читать технику, вооружение, сооружения;
- запускать разрешенные аналитические SQL-запросы;
- смотреть dashboard и reports.

Не может изменять предметные данные.

### `ARMY_COMMANDER`

Командир армии.

Может читать и изменять данные внутри назначенной армии:

- подчиненные формирования;
- военные части;
- подразделения;
- личный состав;
- технику;
- вооружение;
- сооружения.

### `FORMATION_COMMANDER`

Командир формирования: корпус, дивизия или бригада.

Может работать только внутри назначенного формирования и его подчиненной структуры.

### `UNIT_COMMANDER`

Командир военной части.

Может управлять:

- своей частью;
- подразделениями внутри части;
- военнослужащими внутри части;
- техникой и вооружением части;
- сооружениями части.

### `COMPANY_COMMANDER`

Командир роты.

Может:

- видеть свою роту и подчиненные взводы/отделения;
- читать данные военнослужащих в scope;
- обновлять ограниченный набор данных личного состава в scope.

### `PLATOON_COMMANDER`

Командир взвода.

Может:

- видеть свой взвод и отделения;
- читать личный состав взвода;
- обновлять ограниченные данные личного состава в scope.

### `SQUAD_COMMANDER`

Командир отделения.

Может:

- видеть свое отделение;
- читать данные военнослужащих отделения;
- выполнять минимальные обновления в рамках отделения.

### `SOLDIER`

Обычный военнослужащий.

Может:

- видеть свою карточку;
- видеть свое подразделение;
- видеть свою специальность и звание;
- читать ограниченные уведомления.

Не может пользоваться административными и аналитическими разделами.

## CRUD permission matrix

Обозначения:

- `R` - чтение;
- `C` - создание;
- `U` - изменение;
- `D` - удаление;
- `SCOPE` - только внутри command assignment;
- `ALL` - весь округ;
- `SELF` - только собственная карточка.

| Роль | Structure | Personnel | Equipment | Weapons | Buildings | Specialties | Query Terminal | Users/Roles |
|---|---|---|---|---|---|---|---|---|
| `ADMIN_DISTRICT` | CRUD ALL | CRUD ALL | CRUD ALL | CRUD ALL | CRUD ALL | CRUD ALL | all | CRUD |
| `STAFF_ANALYST` | R ALL | R ALL | R ALL | R ALL | R ALL | R ALL | allowed read-only | no |
| `ARMY_COMMANDER` | R/U SCOPE | R/U SCOPE | R/U SCOPE | R/U SCOPE | R/U SCOPE | R ALL | scope queries | no |
| `FORMATION_COMMANDER` | R/U SCOPE | R/U SCOPE | R/U SCOPE | R/U SCOPE | R/U SCOPE | R ALL | scope queries | no |
| `UNIT_COMMANDER` | R/U SCOPE | R/U SCOPE | R/U SCOPE | R/U SCOPE | R/U SCOPE | R ALL | scope queries | no |
| `COMPANY_COMMANDER` | R SCOPE | R/U SCOPE | R SCOPE | R SCOPE | R SCOPE | R ALL | limited | no |
| `PLATOON_COMMANDER` | R SCOPE | R/U SCOPE | R SCOPE | R SCOPE | no | R ALL | no/limited | no |
| `SQUAD_COMMANDER` | R SCOPE | R/U SCOPE | R SCOPE | R SCOPE | no | R ALL | no | no |
| `SOLDIER` | R SELF | R SELF | no | no | no | R SELF | no | no |

## Примеры данных

### Роли

```sql
INSERT INTO roles (code, name, description) VALUES
('ADMIN_DISTRICT', 'Администратор округа', 'Полный доступ к системе'),
('STAFF_ANALYST', 'Аналитик штаба', 'Доступ к аналитике и отчетам без изменения данных'),
('ARMY_COMMANDER', 'Командир армии', 'Командный доступ в пределах назначенной армии'),
('FORMATION_COMMANDER', 'Командир формирования', 'Командный доступ в пределах корпуса, дивизии или бригады'),
('UNIT_COMMANDER', 'Командир военной части', 'Командный доступ в пределах назначенной военной части'),
('COMPANY_COMMANDER', 'Командир роты', 'Доступ в пределах назначенной роты'),
('PLATOON_COMMANDER', 'Командир взвода', 'Доступ в пределах назначенного взвода'),
('SQUAD_COMMANDER', 'Командир отделения', 'Доступ в пределах назначенного отделения'),
('SOLDIER', 'Военнослужащий', 'Доступ только к собственному профилю');
```

### Предопределенные пользователи

Хеши паролей ниже условные. В seed-файле используются BCrypt-хеши.

```sql
INSERT INTO users (username, password_hash, display_name, personnel_id) VALUES
('admin.district', '$2a$10$replace_with_bcrypt_hash', 'Администратор округа', 1),
('analyst.staff', '$2a$10$replace_with_bcrypt_hash', 'Аналитик штаба', 2),
('army.cmd.1', '$2a$10$replace_with_bcrypt_hash', 'Командир армии', 3),
('brigade.cmd.1', '$2a$10$replace_with_bcrypt_hash', 'Командир бригады', 4),
('unit.cmd.1', '$2a$10$replace_with_bcrypt_hash', 'Командир части', 5),
('company.cmd.1', '$2a$10$replace_with_bcrypt_hash', 'Командир роты', 6),
('platoon.cmd.1', '$2a$10$replace_with_bcrypt_hash', 'Командир взвода', 7),
('squad.cmd.1', '$2a$10$replace_with_bcrypt_hash', 'Командир отделения', 8),
('soldier.demo', '$2a$10$replace_with_bcrypt_hash', 'Военнослужащий', 9);
```

### Назначение ролей

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN roles r ON r.code = 'ADMIN_DISTRICT'
WHERE u.username = 'admin.district';

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN roles r ON r.code = 'STAFF_ANALYST'
WHERE u.username = 'analyst.staff';

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN roles r ON r.code = 'UNIT_COMMANDER'
WHERE u.username = 'unit.cmd.1';
```

### Command assignments

```sql
INSERT INTO command_assignments (soldier_id, object_type, object_id, is_primary)
SELECT personnel_id, 'DISTRICT', 1, TRUE
FROM users
WHERE username = 'admin.district';

INSERT INTO command_assignments (soldier_id, object_type, object_id, is_primary)
SELECT personnel_id, 'DISTRICT', 1, TRUE
FROM users
WHERE username = 'analyst.staff';

INSERT INTO command_assignments (soldier_id, object_type, object_id, is_primary)
SELECT personnel_id, 'ARMY', 2, TRUE
FROM users
WHERE username = 'army.cmd.1';

INSERT INTO command_assignments (soldier_id, object_type, object_id, is_primary)
SELECT personnel_id, 'BRIGADE', 5, TRUE
FROM users
WHERE username = 'brigade.cmd.1';

INSERT INTO command_assignments (soldier_id, object_type, object_id, is_primary)
SELECT personnel_id, 'MILITARY_UNIT', 1, TRUE
FROM users
WHERE username = 'unit.cmd.1';

INSERT INTO command_assignments (soldier_id, object_type, object_id, is_primary)
SELECT personnel_id, 'COMPANY', 10, TRUE
FROM users
WHERE username = 'company.cmd.1';

INSERT INTO command_assignments (soldier_id, object_type, object_id, is_primary)
SELECT personnel_id, 'PLATOON', 11, TRUE
FROM users
WHERE username = 'platoon.cmd.1';

INSERT INTO command_assignments (soldier_id, object_type, object_id, is_primary)
SELECT personnel_id, 'SQUAD', 12, TRUE
FROM users
WHERE username = 'squad.cmd.1';

INSERT INTO command_assignments (soldier_id, object_type, object_id, is_primary)
SELECT personnel_id, 'SELF', personnel_id, TRUE
FROM users
WHERE username = 'soldier.demo';
```

## Как определять scope доступа

Решение о доступе состоит из двух шагов.

### Шаг 1. Проверка роли

Например, endpoint:

```http
PUT /api/units/{unitId}/equipment/{typeId}
```

разрешен только ролям:

```text
ADMIN_DISTRICT
ARMY_COMMANDER
FORMATION_COMMANDER
UNIT_COMMANDER
```

Если роль не подходит, backend возвращает `403 Forbidden`.

### Шаг 2. Проверка области

Если роль подходит, backend проверяет, находится ли объект внутри `command_assignments` пользователя.

Пример:

```text
Пользователь: unit.cmd.1
Роль: UNIT_COMMANDER
Assignment: MILITARY_UNIT:1
Действие: изменить equipment_in_units для unit_id = 1
Результат: доступ разрешен
```

Другой пример:

```text
Пользователь: unit.cmd.1
Роль: UNIT_COMMANDER
Assignment: MILITARY_UNIT:1
Действие: изменить equipment_in_units для unit_id = 2
Результат: 403 Forbidden
```

## Проверка scope по текущей иерархии

Для верхней структуры нужно использовать `v_formation_closure` из `selects/formation_closure.sql`.

Если пользователь назначен на армию, корпус, дивизию или бригаду, backend должен считать доступными:

- это формирование;
- подчиненные формирования;
- военные части внутри этих формирований;
- подразделения внутри этих частей;
- личный состав внутри этих подразделений;
- технику, вооружение и сооружения этих частей.

Для нижней структуры нужно использовать `subdivisions.parent_id`.

Если пользователь назначен на роту, он видит:

- эту роту;
- подчиненные взводы;
- подчиненные отделения;
- личный состав внутри них.

Scope-проверки выполняет backend-сервис `AccessScopeService`.

Пример методов:

```java
boolean canReadFormation(Long userId, Long formationId);
boolean canReadUnit(Long userId, Long unitId);
boolean canReadSubdivision(Long userId, Long subdivisionId);
boolean canReadPersonnel(Long userId, Long personnelId);
boolean canUpdateUnitResources(Long userId, Long unitId);
```

Внутри сервис выполняет SQL-проверки по `command_assignments`, `military_formations`, `military_units`, `subdivisions` и `personnel`.

## SQL-функция для централизованной проверки

Для централизованной scope-проверки используется функция:

```sql
CREATE OR REPLACE FUNCTION is_object_in_user_scope(
    p_soldier_id INTEGER,
    p_object_type TEXT,
    p_object_id INTEGER
) RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN
    -- Реализация использует command_assignments и текущую иерархию БД.
    RETURN FALSE;
END;
$$;
```

Backend может вызывать эту функцию из `AccessScopeService` или выполнять эквивалентные SQL-проверки через repository-слой.

## Связь с интерфейсом

Frontend получает текущего пользователя через:

```http
GET /api/auth/me
```

Пример:

```json
{
  "username": "unit.cmd.1",
  "roles": ["UNIT_COMMANDER"],
  "assignments": [
    {
      "objectType": "MILITARY_UNIT",
      "objectId": 1
    }
  ],
  "permissions": [
    "structure:read",
    "personnel:read",
    "personnel:update",
    "equipment:read",
    "equipment:update",
    "weapons:read",
    "weapons:update"
  ]
}
```

UI использует эти данные для:

- показа разделов в sidebar;
- скрытия кнопок create/update/delete;
- ограничения фильтров;
- отображения текущей области командования;
- режима access simulation.

Важно: frontend только улучшает UX. Реальная защита всегда выполняется на backend.

## Предопределенные пользователи

Набор пользователей:

| Username | Роль | Scope | Назначение |
|---|---|---|---|
| `admin.district` | `ADMIN_DISTRICT` | `DISTRICT:1` | полный доступ, пользователи, роли, все данные |
| `analyst.staff` | `STAFF_ANALYST` | `DISTRICT:1` | read-only, reports, query terminal |
| `army.cmd.1` | `ARMY_COMMANDER` | `ARMY:2` | видит только назначенную армию |
| `brigade.cmd.1` | `FORMATION_COMMANDER` | `BRIGADE:5` | видит только бригаду и подчиненные части |
| `unit.cmd.1` | `UNIT_COMMANDER` | `MILITARY_UNIT:1` | управляет частью, техникой и вооружением |
| `platoon.cmd.1` | `PLATOON_COMMANDER` | `PLATOON:11` | видит только взвод |
| `soldier.demo` | `SOLDIER` | `SELF:9` | видит только собственную карточку |

Разные пользователи получают разный набор данных и действий:

```text
admin.district -> видит весь округ
analyst.staff -> видит весь округ, но без кнопок изменения
unit.cmd.1 -> видит только одну часть
soldier.demo -> видит только свой профиль
```

## Что обязательно проверять на backend

Backend должен проверять доступ на каждом изменяющем endpoint'е:

- `POST`;
- `PUT`;
- `PATCH`;
- `DELETE`.

Также нужно проверять read endpoint'ы, если они возвращают данные не всего округа, а конкретной области.

Примеры:

```text
GET /api/personnel/{id}
PUT /api/personnel/{id}
GET /api/units/{id}/equipment
PUT /api/units/{id}/equipment/{typeId}
POST /api/queries/TASK_01/execute
```

Для query terminal каждый запрос должен иметь metadata:

```text
code
title
description
requiredRoles
scopeType
sqlFile
parameters
```

Пример:

```json
{
  "code": "TASK_01",
  "title": "Военные части выбранного округа",
  "requiredRoles": ["ADMIN_DISTRICT", "STAFF_ANALYST", "ARMY_COMMANDER"],
  "scopeType": "FORMATION_TREE",
  "sqlFile": "selects/task1.sql"
}
```

## Итоговая формула

```text
Role answers: what can the user do?
Command assignment answers: where can the user do it?
personnel_id answers: who is this user in the military database?
rank answers: what title should be shown, not what access should be granted?
```

В русской формулировке:

```text
Роль дает действие.
Назначение дает область.
Связь с personnel дает личность.
Звание отображается, но не авторизует.
```
