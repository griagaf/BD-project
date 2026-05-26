# Безопасность

## Аутентификация

Аутентификация реализована через JWT access token и refresh token. Access token используется для API-запросов, refresh token выдаёт новую пару токенов без повторного ввода пароля. Logout инвалидирует refresh token.

## Роли и permissions

Роль определяет разрешённые действия, а `command_assignments` определяет область, в которой эти действия применимы. Звание военнослужащего не выдаёт права доступа.

`ADMIN_DISTRICT` имеет полный доступ к данным округа. В режиме Access Simulation администратор получает временный effective context выбранной роли и области, без изменения реальных ролей.

## Scope

Scope вычисляется из активных записей `command_assignments`. Для командиров учитывается иерархия: назначение на армию открывает подчинённые соединения, части и подразделения; назначение на часть открывает подразделения и связанные ресурсы этой части.

## Backend enforcement

Все операции чтения и изменения проверяются на backend:

- `PermissionService` проверяет action permission.
- `AccessControlService` проверяет область доступа.
- `PermissionEvaluator` используется в `@PreAuthorize`.
- lookup, reports, Query Terminal, dashboard и exports возвращают только данные текущего scope.

Frontend PermissionGuard используется только для качества интерфейса.

Подробное описание access control находится в [access-control.md](access-control.md).
