# Java Coding Conventions — LMS

Conventions for the LMS monolith (`com.lms`). Companion docs:

- `AGENTS.md` — agent workflow and architecture overview
- `docs/business-rules.md` — product behavior (confirmed vs unresolved)

This file defines **how** we write Java code. It does not redefine business rules.

**Stack:** Java 17 · Spring Boot 4 · Maven · Spring Data JPA · PostgreSQL · Flyway · Lombok

---

## 1. General Principles

- Prefer **readability**, **consistency**, and **maintainability** over cleverness.
- Match surrounding code style before introducing a new pattern.
- Avoid unnecessary abstractions, premature generalization, and unused layers.
- Do not invent business behavior for items marked **Unresolved** in
  `docs/business-rules.md`.
- Keep changes scoped to the task; no drive-by refactors.

---

## 2. Naming Conventions

| Kind | Convention | Example |
|------|------------|---------|
| Packages | lowercase, dot-separated | `com.lms.service` |
| Classes / interfaces / enums | UpperCamelCase | `UserService`, `UserRepository`, `UserRole` |
| Methods / variables | lowerCamelCase | `findByEmail`, `instructorId` |
| Constants | UPPER_SNAKE_CASE | `FREE_PRICE` |
| Enum values | UPPER_SNAKE_CASE | `STUDENT`, `IN_PROGRESS` |

### Interfaces

- Name by capability or role: `UserRepository`, not `IUserRepository`.
- Do **not** use an `I` prefix.

### Booleans

- Methods: `isActive()`, `hasFinalQuiz()`, `canEnroll()`, `shouldExpire()`.
- Fields: prefer `active`, `published`, `completed` (map to `is_*` columns as needed).

### Tests

- Class: `UserServiceTest`, `CourseServiceTest` (unit); `*IT` or `*IntegrationTest` for integration.
- Methods: `create_rejectsDuplicateEmail`, `unpublish_setsPublishedFalse`
  (behavior under test + outcome).

---

## 3. DTO Naming

Use **Request** for inbound API payloads and **Response** for outbound payloads.

| Pattern | Example |
|---------|---------|
| Create | `CreateUserRequest`, `CreateCourseRequest` |
| Update | `UpdateUserRequest`, `UpdateCourseRequest` |
| Full response | `UserResponse`, `CourseResponse` |
| Summary / detail variants | `UserSummaryResponse`, `CourseDetailResponse` |

`CreateUserRequest` is valid: class names may be **descriptive noun phrases**,
not only single nouns.

**Avoid:** `UserData`, `UserInfo`, `CourseDTO`, `UserModel` for API contracts.

Place DTOs under `com.lms.dto.<area>` (e.g. `dto.user`, `dto.course`).

Never put `password`, `passwordHash`, or other secrets on response DTOs.

---

## 4. Layer Responsibilities

Technical-layer packages under `com.lms` (Approach A):

| Layer | Responsibility |
|-------|----------------|
| `controller` | HTTP mapping, status codes, `@Valid` on request bodies; call services only |
| `service` | Business rules, orchestration, `@Transactional` boundaries |
| `repository` | Spring Data JPA persistence and derived/custom queries |
| `entity` | Persistence state mapped to Flyway tables |
| `dto` | API contracts |
| `mapper` | Entity ↔ DTO conversion |
| `exception` | Domain/API exceptions + `@RestControllerAdvice` |
| `configuration` | Spring beans and security/config |
| `util` | Genuinely shared helpers only |

Rules:

- Prefer **constructor injection** (explicit constructors or required-args style).
- Controllers **must not** access repositories.
- Controllers **must not** return JPA entities.
- Services must not depend on HTTP types (`HttpServletRequest`, etc.).
- No circular service dependencies.
- Do not add feature-root packages (`com.lms.user`, `com.lms.course`) alongside layers.

---

## 5. Spring Boot and REST Conventions

### Endpoints

- Resource-oriented paths: `/api/users`, `/api/courses`, `/api/courses/{id}/unpublish`.
- Use HTTP verbs by meaning: `GET` read, `POST` create / non-idempotent actions,
  `PUT`/`PATCH` update, `DELETE` only when hard-delete is an explicit product rule.

### Validation and status codes

- Validate request DTOs with Jakarta Bean Validation (`@NotBlank`, `@Email`, …).
- Enforce cross-field / domain rules in services (`BusinessRuleException`, etc.).
- Prefer consistent statuses:
  - `201` create
  - `200` successful read/update
  - `400` validation / business rule violation
  - `404` missing resource
  - `409` conflict (e.g. duplicate email)

### Errors and exposure

- Handle errors in `GlobalExceptionHandler` (or equivalent `@RestControllerAdvice`).
- Never expose entities, stack traces, or sensitive fields in API responses.
- Map persistence conflicts to clear API errors where practical.

---

## 6. JPA and Entity Conventions

- Treat Flyway as the **only** schema owner.
- Never edit applied migrations (`V1`, `V2`, …).
- Keep `spring.jpa.hibernate.ddl-auto=validate`.
- **Never** use `ddl-auto=update`.

### Mapping

- Explicit `@Table` / `@Column` when names differ from defaults.
- Application-assigned UUID primary keys (no DB UUID defaults).
- Prefer unidirectional associations; use `FetchType.LAZY` for associations.
- Avoid `CascadeType.ALL` / `REMOVE` by default.
- Align nullability and uniqueness with DB constraints; still validate in services.

### Lombok on entities

- Prefer `@Getter`, selective `@Setter`, and an explicit / Lombok no-args constructor
  suitable for JPA.
- Do **not** use `@Data` on entities.
- Avoid indiscriminate `@Builder` and `@AllArgsConstructor` on entities.
- Do not generate equals/hashCode/toString that traverse associations
  (except composite key embeddables with ID fields only).

### Transactions

- Put `@Transactional` on service methods (class-level `readOnly = true` with
  write methods overriding is fine).
- Do not open transactions in controllers.
- Keep transactional methods focused; avoid long external I/O inside them.

---

## 7. Exception and Validation Conventions

- Prefer specific types: `ResourceNotFoundException`, `ConflictException`,
  `BusinessRuleException` (extend as needed; keep names clear).
- Do not swallow exceptions (`catch (Exception e) { }` or log-and-ignore without decision).
- Do not use exceptions for normal control flow when an `Optional` / boolean
  query is clearer **inside** a service—but API boundaries may translate
  absences to 404.
- Validation split:
  - **DTO:** shape, required fields, basic formats
  - **Service:** uniqueness, roles, MVP free-course price, ownership (when auth exists)
  - **DB:** last line of defense (unique / check constraints)

---

## 8. Java Formatting

- Indentation: **four spaces** (no tabs in new code; match file if editing mixed legacy).
- One top-level public type per file.
- Explicit imports; no unused imports; avoid star imports except static test utilities when already conventional.
- Standard brace style (K&R / Spring-style): opening brace on the same line.
- Prefer clear names over dense comments.
- Comments explain **why**, not what the code already says.
- Use Javadoc for non-obvious public APIs and documented service invariants
  (e.g. Final Assessment same-course checks)—not for every getter.

---

## 9. Lombok Conventions

| Prefer | Avoid on entities |
|--------|-------------------|
| `@Getter` | `@Data` |
| Selective `@Setter` / `@Setter(AccessLevel.NONE)` for DB-managed fields | Blanket mutable `@Setter` on every field without thought |
| `@NoArgsConstructor` appropriate for JPA | Relationship-aware `@EqualsAndHashCode` / `@ToString` |
| `@EqualsAndHashCode` on ID-only embeddables | `@Builder` / `@AllArgsConstructor` by default |

DTOs may be Java `record`s (preferred for request/response) without Lombok.

---

## 10. Testing Conventions

- **Unit tests:** services/mappers with mocked repositories (`UserServiceTest`).
- **Integration / context tests:** Spring Boot test slice or `@SpringBootTest`
  when verifying wiring or persistence (`LmsApplicationTests`).
- Assert business outcomes and edge cases: duplicate email, paid price rejected,
  not-found, invalid instructor role.
- Do not test private methods directly; test through the public API of the class.
- Do not assert on incidental log output or private field state.
- Name tests after behavior, not implementation steps.

---

## 11. Cursor Agent Workflow

Before implementing a feature:

1. Read `AGENTS.md`, `docs/business-rules.md`, and **this file**.
2. Inspect existing packages for reusable services, DTOs, mappers, and exceptions.
3. Follow confirmed business rules; do not invent unresolved ones.
4. Do not add unrequested dependencies, schema changes, or placeholder classes.
5. Do not modify applied Flyway migrations or switch to `ddl-auto=update`.
6. After implementation, run:

   ```bash
   ./mvnw -q -DskipTests compile
   ```

7. Run focused tests when feasible; if starting the app for Flyway/Hibernate
   validation, report the **actual** result (success or exact failure).
8. Prefer reporting schema limitations over inventing columns or lifecycles
   (e.g. archive vs unpublish).

---

## 12. Examples

### Preferred

```text
com.lms.dto.user.CreateUserRequest
com.lms.dto.user.UpdateUserRequest
com.lms.dto.user.UserResponse
com.lms.dto.user.UserSummaryResponse
com.lms.dto.course.CourseDetailResponse
com.lms.service.CourseService
com.lms.repository.EnrollmentRepository
com.lms.entity.QuizAttemptStatus.IN_PROGRESS
UserServiceTest.create_rejectsDuplicateEmail()
user.isActive()
course.isPublished()
```

### Discouraged

```text
com.lms.dto.UserData
com.lms.dto.UserInfo
com.lms.dto.CourseDTO
IUserService
ICourseRepository
userServiceImpl / UserSvc
getUserListData()
handleStuff()
@Data class Course { ... }          // on a JPA entity
controllers calling CourseRepository directly
returning Course entity from REST endpoints
editing V1__initial_schema.sql after it was applied
spring.jpa.hibernate.ddl-auto=update
```
