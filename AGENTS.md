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
