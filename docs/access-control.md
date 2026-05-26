# Access Control

## Принцип

Права доступа состоят из двух частей:

- role permissions определяют, какие действия разрешены;
- command assignments определяют, в какой области эти действия применимы.

Звание военнослужащего не выдаёт права доступа.

## Enforcement

Backend проверяет доступ во всех критичных сценариях:

- CRUD;
- lookup endpoints;
- hierarchy tree;
- Query Terminal;
- reports;
- alerts;
- dashboard;
- exports;
- access simulation.

Frontend PermissionGuard используется только для отображения интерфейса.

## ADMIN_DISTRICT

`ADMIN_DISTRICT` имеет полный доступ к округу. При включённом Access Simulation effective context временно заменяется выбранной ролью и scope, но реальные роли пользователя не изменяются.

## Scope-Aware Lookups

Lookup endpoints возвращают только объекты, которые пользователь может читать. Для Query Terminal lookup званий поддерживает category-aware filtering: офицерские запросы получают офицерские звания, запросы по рядовому и сержантскому составу получают соответствующие звания.

## Row-Level Guard

Даже если SQL-запрос возвращает объект за пределами области доступа, application layer повторно проверяет `canRead` по `objectType` и `objectId`.
