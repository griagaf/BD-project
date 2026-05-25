# Архитектура

## Общая схема

Система состоит из PostgreSQL, Spring Boot API и React frontend. Backend отвечает за аутентификацию, авторизацию, бизнес-валидацию, выполнение SQL-запросов, аудит и формирование DTO. Frontend отображает tactical command interface и не принимает решений о доступе самостоятельно.

## Backend

Backend разделён на модули `auth`, `user`, `security`, `hierarchy`, `unit`, `personnel`, `equipment`, `weapon`, `building`, `intelligence`, `alert`, `dashboard`, `report`, `audit` и `common`.

Обычный CRUD выполняется через Spring Data JPA и сервисный слой. Аналитические запросы, паспорта типов, lookup-и, дерево и отчёты используют `NamedParameterJdbcTemplate`, потому что эти операции опираются на готовую SQL-модель и агрегаты.

## Frontend

Frontend построен по модульному подходу: `pages`, `features`, `shared`, `app`. API-вызовы изолированы в feature-level clients и shared clients. TanStack Query управляет серверным состоянием, Zustand хранит client-side auth/simulation state, i18next обеспечивает локализацию.

## Интеграционный поток

1. Пользователь проходит login и получает access/refresh token.
2. Frontend отправляет access token в `Authorization: Bearer`.
3. Backend собирает `UserContext`, роли, permissions и command scope.
4. Сервисный слой проверяет права через `PermissionService` и scope через `AccessControlService`.
5. DTO возвращаются уже отфильтрованными по области доступа.
