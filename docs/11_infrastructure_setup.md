# TACTICAL DISTRICT COMMAND: infrastructure setup

## Назначение

Документ описывает infrastructure setup для **TACTICAL DISTRICT COMMAND**.

Цель инфраструктуры:

```text
docker compose up --build
```

Поднимаемые сервисы:

- PostgreSQL;
- Spring Boot backend;
- React/Vite frontend, раздаваемый через Nginx.

## Repository layout

```text
.
├── backend/
│   ├── Dockerfile
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── src/
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   └── src/
├── docker-compose.yml
├── .env.example
├── create.txt
├── military_district_seed.sql
├── selects/
├── triggers/
└── docs/
```

## 1. `docker-compose.yml`

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: tactical-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "${POSTGRES_PORT}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 10
    networks:
      - tactical-network

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: tactical-backend
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SERVER_PORT: 8080
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: ${POSTGRES_DB}
      DB_USER: ${POSTGRES_USER}
      DB_PASSWORD: ${POSTGRES_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      JWT_ACCESS_TTL_MINUTES: ${JWT_ACCESS_TTL_MINUTES}
      JWT_REFRESH_TTL_DAYS: ${JWT_REFRESH_TTL_DAYS}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS}
    ports:
      - "${BACKEND_PORT}:8080"
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:8080/actuator/health | grep UP || exit 1"]
      interval: 15s
      timeout: 5s
      retries: 10
    networks:
      - tactical-network

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
      args:
        VITE_API_URL: ${VITE_API_URL}
    container_name: tactical-frontend
    restart: unless-stopped
    depends_on:
      backend:
        condition: service_healthy
    ports:
      - "${FRONTEND_PORT}:80"
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost/health || exit 1"]
      interval: 15s
      timeout: 5s
      retries: 10
    networks:
      - tactical-network

volumes:
  postgres_data:

networks:
  tactical-network:
    driver: bridge
```

## 2. Backend Dockerfile

File: `backend/Dockerfile`

```dockerfile
FROM gradle:8.14.3-jdk21-alpine AS build

WORKDIR /app

COPY settings.gradle.kts build.gradle.kts ./
COPY src ./src

RUN gradle --no-daemon clean bootJar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S tactical && adduser -S tactical -G tactical

COPY --from=build /app/build/libs/*.jar app.jar

USER tactical

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

## 3. Frontend Dockerfile

File: `frontend/Dockerfile`

```dockerfile
FROM node:22-alpine AS build

WORKDIR /app

ARG VITE_API_URL
ENV VITE_API_URL=${VITE_API_URL}

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

## Frontend Nginx config

File: `frontend/nginx.conf`

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    location /health {
        access_log off;
        add_header Content-Type text/plain;
        return 200 "OK";
    }

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /assets/ {
        try_files $uri =404;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

## 4. PostgreSQL container

PostgreSQL service uses:

```text
image: postgres:16-alpine
container: tactical-postgres
database: tactical_db
user: tactical
port: 5432
volume: postgres_data
```

The database schema is managed through Flyway migrations executed by the backend on startup.

## 5. Volumes

```yaml
volumes:
  postgres_data:
```

Volume purpose:

- persists PostgreSQL data between container restarts;
- isolates database files from application containers;
- allows `docker compose down` without losing data;
- allows `docker compose down -v` for full reset.

Useful commands:

```bash
docker compose down
docker compose down -v
docker compose logs -f postgres
docker compose logs -f backend
docker compose logs -f frontend
```

## 6. Healthchecks

### PostgreSQL

```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
  interval: 10s
  timeout: 5s
  retries: 10
```

### Backend

Backend uses Spring Boot Actuator:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true
```

Docker healthcheck:

```yaml
healthcheck:
  test: ["CMD-SHELL", "wget -qO- http://localhost:8080/actuator/health | grep UP || exit 1"]
  interval: 15s
  timeout: 5s
  retries: 10
```

### Frontend

Nginx exposes:

```http
GET /health
```

Docker healthcheck:

```yaml
healthcheck:
  test: ["CMD-SHELL", "wget -qO- http://localhost/health || exit 1"]
  interval: 15s
  timeout: 5s
  retries: 10
```

## 7. `.env.example`

```dotenv
POSTGRES_DB=tactical_db
POSTGRES_USER=tactical
POSTGRES_PASSWORD=tactical_password
POSTGRES_PORT=5432

BACKEND_PORT=8080
FRONTEND_PORT=3000

VITE_API_URL=http://localhost:8080
CORS_ALLOWED_ORIGINS=http://localhost:3000

JWT_SECRET=change-this-secret-to-a-long-random-value-at-least-32-chars
JWT_ACCESS_TTL_MINUTES=15
JWT_REFRESH_TTL_DAYS=14
```

## 8. Spring profiles

Profiles:

```text
dev
docker
test
```

### `application.yml`

```yaml
spring:
  application:
    name: tactical-district-command
  profiles:
    default: dev

server:
  port: ${SERVER_PORT:8080}

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true
```

### `application-dev.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tactical_db
    username: tactical
    password: tactical_password
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration

app:
  security:
    jwt:
      secret: ${JWT_SECRET:dev-secret-dev-secret-dev-secret-dev-secret}
      access-ttl-minutes: ${JWT_ACCESS_TTL_MINUTES:15}
      refresh-ttl-days: ${JWT_REFRESH_TTL_DAYS:14}
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

### `application-docker.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration

app:
  security:
    jwt:
      secret: ${JWT_SECRET}
      access-ttl-minutes: ${JWT_ACCESS_TTL_MINUTES:15}
      refresh-ttl-days: ${JWT_REFRESH_TTL_DAYS:14}
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS}
```

### `application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tactical_test_db
    username: tactical
    password: tactical_password
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration

app:
  security:
    jwt:
      secret: test-secret-test-secret-test-secret-test-secret
      access-ttl-minutes: 15
      refresh-ttl-days: 1
```

## 9. Flyway migrations

Flyway manages schema, views, triggers, security tables and seed data.

Migration layout:

```text
backend/src/main/resources/db/migration/
  V001__schema.sql
  V002__views.sql
  V003__triggers.sql
  V004__security_tables.sql
  V005__audit_alert_report_tables.sql
  V006__demo_seed.sql
```

### V001 schema

Contains schema from:

```text
create.txt
```

### V002 views

Contains SQL from:

```text
selects/formation_closure.sql
selects/personnel_full.sql
selects/personnel_specialities.sql
selects/unit_equipment.sql
selects/unit_weapons.sql
selects/buildings_usage.sql
```

### V003 triggers

Contains trigger functions and triggers from:

```text
triggers/update_triggers/
triggers/delete_triggers/
```

### V004 security tables

Contains:

```text
users
roles
user_roles
command_assignments
refresh_tokens
```

### V005 audit, alerts and reports

Contains:

```text
audit_events
alerts
generated_reports
```

### V006 demo seed

Contains:

```text
military_district_seed.sql
roles seed
demo users seed
command assignments seed
```

## Liquibase option

Flyway is the default migration tool.

Liquibase can use the same migration sequence:

```text
001-schema
002-views
003-triggers
004-security
005-audit-alert-report
006-demo-seed
```

When Liquibase is used, the backend disables Flyway:

```yaml
spring:
  flyway:
    enabled: false
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
```

## 10. Seed demo data

Seed data includes:

- military district structure;
- personnel;
- ranks;
- specialties;
- equipment;
- weapons;
- buildings;
- role records;
- demo users;
- command assignments.

Demo users:

```text
admin.district
analyst.staff
army.cmd.1
brigade.cmd.1
unit.cmd.1
company.cmd.1
platoon.cmd.1
squad.cmd.1
soldier.demo
```

Passwords are stored as BCrypt hashes.

Seed migration order:

```text
1. domain schema
2. domain seed
3. views
4. triggers
5. auth tables
6. roles
7. users
8. user_roles
9. command_assignments
```

## 11. Run command

Create `.env` from `.env.example`:

```bash
cp .env.example .env
```

Start:

```bash
docker compose up --build
```

URLs:

```text
Frontend: http://localhost:3000
Backend:  http://localhost:8080
Swagger:  http://localhost:8080/swagger-ui/index.html
Health:   http://localhost:8080/actuator/health
Postgres: localhost:5432
```

Stop:

```bash
docker compose down
```

Reset database:

```bash
docker compose down -v
docker compose up --build
```

## 12. README setup instructions

README setup section:

```md
## Setup

### Requirements

- Docker
- Docker Compose

### Environment

Copy `.env.example` to `.env`:

\`\`\`bash
cp .env.example .env
\`\`\`

### Start

\`\`\`bash
docker compose up --build
\`\`\`

### URLs

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Backend health: http://localhost:8080/actuator/health

### Stop

\`\`\`bash
docker compose down
\`\`\`

### Reset database

\`\`\`bash
docker compose down -v
docker compose up --build
\`\`\`
```

## Operational commands

View logs:

```bash
docker compose logs -f
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f postgres
```

Open database shell:

```bash
docker exec -it tactical-postgres psql -U tactical -d tactical_db
```

Check containers:

```bash
docker compose ps
```

Rebuild only backend:

```bash
docker compose up --build backend
```

Rebuild only frontend:

```bash
docker compose up --build frontend
```
