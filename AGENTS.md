# AGENTS.md — LMS

Project-specific instructions for coding agents working on this repository.

## Coding conventions

Follow `docs/java-coding-conventions.md` for Java naming, layering, REST/JPA,
Lombok, testing, and formatting conventions. Do not contradict that document
without an explicit project decision.

## Stack

- Java 17
- Spring Boot 4.0.x
- Maven (`./mvnw`)
- PostgreSQL
- Flyway for schema changes
- JPA / Hibernate with `spring.jpa.hibernate.ddl-auto=validate`
- Lombok (entities use `@Getter`, selective `@Setter`, protected
  `@NoArgsConstructor`; do **not** use `@Data` on JPA entities)

## Architecture (Approach A — technical layers)

Root package: `com.lms`

```
com.lms/
  entity/
  repository/
  service/          # business logic (to be implemented)
  dto/…             # request/response DTOs by area
  mapper/           # entity ↔ DTO conversion
  controller/       # REST endpoints
  configuration/
  exception/
  util/
```

Do **not** introduce feature-based root packages (`user/`, `course/`, …)
alongside these layers. Nested packages under `dto/` by area are allowed.

## Layer responsibilities

- **Controller**: thin HTTP adapters; validation annotations on DTOs;
  call services only. No repository access. Do not expose JPA entities.
- **Service**: business workflows and authorization checks. Follow
  `docs/business-rules.md`. No circular service dependencies.
- **Repository**: Spring Data JPA only; no business rules.
- **Entity**: persistence mapping to Flyway-owned tables.
- **DTO / Mapper**: API shapes and conversions; keep DTOs separate from
  entities; put conversion in mapper classes.

## Business rules

`docs/business-rules.md` is the canonical source for product behavior.
Distinguish **Confirmed** vs **Unresolved** there. Do not invent rules
for unresolved items; ask or leave explicitly undecided.

## Flyway safety

- Add new migrations (`V3__…`, …). Never edit applied migrations
  (`V1`, `V2`, …) after they have run in shared environments.
- Keep Hibernate on `validate`. Do not use `ddl-auto=update` to bypass
  migrations.
- Preserve existing data; backfill carefully when adding NOT NULL columns.

## Verification

After persistence or meaningful application changes:

1. `./mvnw -q -DskipTests compile`
2. When schema or mapping changes: start the app against PostgreSQL so
   Flyway and Hibernate validation run

Do not treat compile success alone as proof of schema compatibility.

## Scope discipline

- Prefer small, task-scoped changes.
- Do not add unrelated dependencies or placeholder classes.
- Paid enrollment / payment and assignment submission/grading are deferred
  unless a task explicitly requests them.

## Documentation map

| File | Use |
|------|-----|
| `docs/architecture.md` | Stack, packages, Flyway, JWT, API overview |
| `docs/business-rules.md` | Product behavior (Confirmed vs Unresolved) |
| `docs/development-status.md` | What is done, gaps, next tasks |
| `docs/java-coding-conventions.md` | Java naming, REST, tests |
| `scripts/seed/README.md` | Local catalog seed SQL |

Sibling frontend: `platform-ui` (separate Git repo). UI docs: `platform-ui/docs/frontend.md`.

## Local environment (variable names only)

Copy `.env.example` → `.env`. Required for a normal run:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — JDBC to Compose Postgres
- `POSTGRES_PASSWORD` — Compose service only
- `JWT_SECRET`, `JWT_EXPIRATION_MS`
- `CORS_ALLOWED_ORIGINS` — include the Vite dev origin (e.g. `http://localhost:5173`)

Postgres: `docker compose up -d` from repo root. App runs on the **host**, not in Compose.

Spring Boot does **not** auto-load `.env`:

```bash
set -a && source .env && set +a && ./mvnw spring-boot:run
```

## Commands

```bash
./mvnw -q -DskipTests compile   # after Java changes
./mvnw test                       # full unit + integration (currently 125 tests)
```

After Flyway or entity changes, start the app once against Postgres (Flyway + Hibernate `validate`).

## Auth and API pointers

- Registration: `firstName`, `lastName`, `phoneNumber`, `dateOfBirth`, `email`, `password` → `POST /api/auth/register`.
- Login: `identifier` + `password` (email or phone); legacy `email` field accepted server-side.
- Progress lives under `/api/courses/{courseId}/.../progress` — requires same enrollment as lesson content.
- Do not commit secrets, `.env`, or database dumps.

## Continuation

Read `docs/development-status.md` for branch context, known stale-server issues, and recommended next work.
