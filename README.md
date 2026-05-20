# TACTICAL DISTRICT COMMAND

**TACTICAL DISTRICT COMMAND** — информационная система управления военным округом. Система объединяет иерархию формирований, военные части, подразделения, личный состав, технику, вооружение, сооружения, предупреждения, отчёты, аудит и аналитический Query Terminal.

## Возможности

- JWT-аутентификация, refresh token flow и backend-side контроль доступа.
- Ролевая модель с областью командования через `command_assignments`.
- Tactical Dashboard с readiness summary, предупреждениями и деловыми событиями.
- Focus Tree для навигации по структуре округа.
- CRUD для личного состава, иерархии, частей, подразделений, техники, вооружения, сооружений и пользователей.
- Справочники категорий и типов техники/вооружения с динамическими атрибутами.
- Intelligence Query Terminal на основе SQL-запросов из каталога `selects`.
- Smart Mission Report с составом, готовностью, проблемами и рекомендациями.
- Access Simulation Mode для просмотра интерфейса в пределах другой роли и области.
- Русский интерфейс по умолчанию, английский язык как дополнительный режим.

## Технологии

- Backend: Java 21, Spring Boot 4, Spring Security, JWT, Spring Data JPA, NamedParameterJdbcTemplate, MapStruct, Lombok, Flyway.
- Database: PostgreSQL, SQL schema, trigger functions, constraints, seed data.
- Frontend: React 19, TypeScript, Vite, Tailwind CSS, shadcn/ui-подход, TanStack Query, Zustand, React Router, Recharts, Framer Motion, Lucide Icons, i18next.
- Infrastructure: Docker, Docker Compose, Nginx для frontend.

## Структура репозитория

```text
backend/                 Spring Boot API
frontend/                React + Vite приложение
create.sql               исходная SQL-модель БД
selects/                 аналитические SELECT-запросы
triggers/                функции и триггеры БД
docs/                    документация системы
docker-compose.yml       запуск PostgreSQL, backend и frontend
.env.example             пример переменных окружения
```

## Запуск

```bash
docker compose up --build
```

После запуска:

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Healthcheck: http://localhost:8080/actuator/health

Полный сброс базы:

```bash
docker compose down -v
docker compose up --build
```

## Переменные окружения

Базовые значения указаны в `.env.example`: параметры PostgreSQL, JDBC URL, JWT secret, сроки жизни токенов, профили Spring и адрес API для frontend.

## Миграции и данные

Flyway применяет схему, триггеры, представления, динамические атрибуты и русскоязычный набор данных. `create.sql` используется как источник структуры, каталог `triggers` отражён в миграциях, а запросы из `selects` доступны через Query Terminal.

## Роли

- `ADMIN_DISTRICT` — полный доступ к округу и администрированию.
- `STAFF_ANALYST` — аналитический доступ без изменения оперативных данных.
- `ARMY_COMMANDER`, `FORMATION_COMMANDER`, `UNIT_COMMANDER`, `COMPANY_COMMANDER`, `PLATOON_COMMANDER`, `SQUAD_COMMANDER` — доступ в пределах назначенной области командования.
- `SOLDIER` — доступ к личной карточке и разрешённым связанным данным.

Права проверяются на backend. Frontend скрывает или отключает действия только для удобства.

## Demo Accounts

Все демонстрационные учётные записи используют пароль `password`.

- `admin.district`
- `analyst.staff`
- `army.cmd.1`
- `brigade.cmd.1`
- `unit.cmd.1`
- `company.cmd.1`
- `platoon.cmd.1`
- `squad.cmd.1`
- `soldier.demo`

## Документация

- [Архитектура](docs/architecture.md)
- [Безопасность и доступ](docs/security.md)
- [База данных](docs/database.md)
- [Модули системы](docs/modules)

## Troubleshooting

- Если backend не стартует после изменения миграций, выполните `docker compose down -v` и запустите систему заново.
- Если frontend показывает старые данные после смены роли или режима симуляции, выйдите из системы и войдите снова: клиент очищает query cache при logout/login/simulation switch.
- Если порт занят, измените опубликованные порты в `docker-compose.yml`.
