# TACTICAL DISTRICT COMMAND

**TACTICAL DISTRICT COMMAND** is a backend and database-centered command system for managing a military district structure, personnel, units, equipment, weapons, buildings, specialties, reports, alerts, audit events, and analytical SQL queries.

## Repository Contents

```text
.
├── create.txt
├── military_district_seed.sql
├── selects/
├── triggers/
├── docs/
└── README.md
```

## Database Files

- [`create.txt`](create.txt) - database schema.
- [`military_district_seed.sql`](military_district_seed.sql) - seed data.
- [`selects/`](selects/) - SQL views and analytical queries.
- [`triggers/`](triggers/) - database functions and triggers.

## Documentation

- [`docs/01_application_architecture.md`](docs/01_application_architecture.md) - application architecture.
- [`docs/02_access_control.md`](docs/02_access_control.md) - roles, users, command assignments, and access scope.
- [`docs/03_backend_security_architecture.md`](docs/03_backend_security_architecture.md) - JWT authentication and backend authorization architecture.
- [`docs/04_backend_application_design.md`](docs/04_backend_application_design.md) - backend package structure, REST API, DTO, services, repositories, OpenAPI, pagination, filtering, and audit.
- [`docs/05_intelligence_query_terminal.md`](docs/05_intelligence_query_terminal.md) - Intelligence Query Terminal, query templates, backend execution, access control, CSV export, and frontend components.
- [`docs/06_frontend_architecture.md`](docs/06_frontend_architecture.md) - frontend architecture, Feature-Sliced Design, routing, auth store, API client, layout, pages, components, and CRUD UI patterns.
- [`docs/07_focus_tree.md`](docs/07_focus_tree.md) - Focus Tree module, strategic tree, focus mode, chain mode, lazy loading, scoped tree API, passport card, and React components.
- [`docs/08_tactical_dashboard_alerts_readiness.md`](docs/08_tactical_dashboard_alerts_readiness.md) - Tactical Dashboard, Alert Center, Alert to Action, Readiness Radar, scoped metrics, backend services, endpoints, DTO, and frontend components.
- [`docs/09_smart_mission_report.md`](docs/09_smart_mission_report.md) - Smart Mission Report, tactical report generation, report DTO, rule-based recommendations, export CSV/PDF, frontend preview, Query Terminal and Alert Center integration.
- [`docs/10_crud_modules.md`](docs/10_crud_modules.md) - CRUD modules, endpoints, frontend forms, validation rules, permission guards, backend checks, refetch strategy, error handling, audit log, and soft delete rules.
- [`docs/11_infrastructure_setup.md`](docs/11_infrastructure_setup.md) - Docker Compose setup, backend/frontend Dockerfiles, PostgreSQL, volumes, healthchecks, environment variables, Spring profiles, migrations, seed data, and run commands.
- [`docs/12_implementation_plan.md`](docs/12_implementation_plan.md) - staged implementation plan for MVP, Core version, Visual version, and Final defense version.

## Setup

### Requirements

- Docker
- Docker Compose

### Environment

Create `.env` from `.env.example`:

```bash
cp .env.example .env
```

### Start

```bash
docker compose up --build
```

### URLs

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Backend health: http://localhost:8080/actuator/health

### Demo Users

All seeded demo users use password `password`.

- `admin.district`
- `analyst.staff`
- `army.cmd.1`
- `brigade.cmd.1`
- `unit.cmd.1`
- `company.cmd.1`
- `platoon.cmd.1`
- `squad.cmd.1`
- `soldier.demo`

### Stop

```bash
docker compose down
```

### Reset Database

```bash
docker compose down -v
docker compose up --build
```

## Core Backend Stack

- Java 21+
- Spring Boot 4.x
- Gradle Kotlin DSL
- Spring Security
- JWT
- PostgreSQL
- Spring Data JPA
- NamedParameterJdbcTemplate
- MapStruct
- OpenAPI / Swagger
- React 19+
- TypeScript
- Vite
- Tailwind CSS
- shadcn/ui
- TanStack Query
- Zustand
- TanStack Table
- React Hook Form
- Zod
- Recharts
- Framer Motion
- Lucide Icons

## Data Access Rules

```text
Standard CRUD -> Spring Data JPA
Complex SQL queries -> NamedParameterJdbcTemplate
Authentication -> JWT
Authorization -> roles + command_assignments
Audit -> audit_events
```

## Main Backend Modules

- `auth`
- `user`
- `personnel`
- `hierarchy`
- `unit`
- `equipment`
- `weapon`
- `building`
- `specialty`
- `intelligence`
- `alert`
- `report`
- `dashboard`
- `security`
- `audit`
- `common`
