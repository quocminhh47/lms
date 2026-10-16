# LMS backend architecture

Monolithic Spring Boot REST API backed by PostgreSQL. Schema is owned by Flyway;
Hibernate validates entities only (`spring.jpa.hibernate.ddl-auto=validate`).

## Stack and versions

| Component | Version / note |
|-----------|----------------|
| Java | 17 (`pom.xml`) |
| Spring Boot | 4.0.x (parent `4.0.8`) |
| Build | Maven Wrapper (`./mvnw`) |
| Database | PostgreSQL 16.10 (Docker Compose image) |
| Migrations | Flyway (`src/main/resources/db/migration/`) |
| Auth | Stateless JWT (Bearer), BCrypt passwords |

## Repository layout

```
lms/
  src/main/java/com/lms/
    configuration/     # Security, CORS, JWT filter wiring
    controller/        # REST adapters (thin)
    dto/               # Request/response by area (auth, course, user, …)
    entity/            # JPA entities
    exception/         # GlobalExceptionHandler, domain exceptions
    mapper/            # Entity ↔ DTO
    repository/        # Spring Data JPA
    security/          # JWT filter, entry points
    service/           # Business logic and authorization
    util/              # e.g. PhoneNumbers normalization
  src/main/resources/db/migration/
  docs/                # business-rules, conventions, this file
  scripts/seed/        # Optional catalog seed SQL
  docker-compose.yml   # Postgres only (app runs on host)
  .env.example         # Variable names for local run (no secrets in repo)
```

**Approach A — technical layers:** root package is `com.lms` with layer folders only.
Do not add parallel feature root packages (`user/`, `course/`). See `AGENTS.md`.

## Database schema (Flyway)

| Migration | Purpose |
|-----------|---------|
| `V1__initial_schema.sql` | Core tables: `users`, `courses`, `sections`, `lessons`, `enrollments`, `lesson_progress`, quiz/assessment/certificate tables |
| `V2__assessment_lifecycle.sql` | Quiz attempts, `courses.final_quiz_id`, lifecycle enums |
| `V3__course_thumbnail_user_profile_fields.sql` | `courses.thumbnail_url`, `users.phone`, `users.date_of_birth` |
| `V4__user_first_last_name_unique_phone.sql` | `users.first_name`, `users.last_name`; phone normalized; `uq_users_phone` |

Important constraints:

- `users`: unique `email`; unique `phone` (many NULLs allowed); `full_name` retained
- `enrollments`: `UNIQUE (user_id, course_id)`; status check
- `lesson_progress`: `UNIQUE (user_id, lesson_id)`
- UUID PKs are **application-assigned** (no DB UUID default)

Never edit migrations that have already run in shared environments; add `V5__…` instead.

## Authentication and authorization

- **Register** `POST /api/auth/register` — public; always creates `STUDENT`. Fields:
  `email`, `password`, `firstName`, `lastName`, `phoneNumber`, `dateOfBirth`.
- **Login** `POST /api/auth/login` — public; body uses `identifier` (email or phone)
  and `password`. Legacy JSON property `email` is accepted as an alias for `identifier`.
- **Session** `GET /api/auth/me` — authenticated; returns current user profile.
- **JWT**: configured via `JWT_SECRET`, `JWT_EXPIRATION_MS`. Clients send
  `Authorization: Bearer <token>`.
- **Method security**: `@PreAuthorize` on admin-only mutating course/section/lesson/user APIs.
- **HTTP security** (`SecurityConfig`): register/login and **GET** catalog routes
  (`/api/courses/**`, `/api/lessons/*`) are permit-all at the filter level.
  Enrollment, progress, lesson content with `contentUrl`, and other mutations require
  authentication; **authorization** (enrollment, role, published course) is enforced in services.

CORS: `CORS_ALLOWED_ORIGINS` (comma-separated). Include the Vite dev origin
(e.g. `http://localhost:5173`).

## API surface (implemented)

Base path: `/api`. Controllers use validation on DTOs; errors via `GlobalExceptionHandler`.

| Area | Base path | Notes |
|------|-----------|--------|
| Auth | `/api/auth` | register, login, me |
| Users | `/api/users` | ADMIN create/list; self or ADMIN update |
| Courses | `/api/courses` | ADMIN mutate; published list/detail public |
| Sections | `/api/courses/{courseId}/sections` | ADMIN mutate; GET public for published |
| Lessons | `/api/courses/.../sections/{sectionId}/lessons` | ADMIN mutate; summaries public; content gated |
| Lesson lookup | `/api/lessons/{lessonId}` | Public route; content gating in service |
| Enrollments | `/api/courses/{courseId}/enrollments` | POST enroll (auth) |
| My enrollments | `/api/enrollments/me` | GET list and by id |
| Progress | `/api/courses/{courseId}/progress`, `.../lessons/{lessonId}/progress` | GET/PUT/complete (auth + enrollment) |

Product rules (enrollment, gating, progress, quizzes) are specified in
`docs/business-rules.md`. Quiz attempt/submit APIs and certificates are **not**
implemented in services yet despite schema support.

## Local development

1. Copy `.env.example` → `.env` and set `POSTGRES_PASSWORD`, `DB_*`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`.
2. `docker compose up -d` (Postgres on `localhost:5432`).
3. Spring Boot does **not** load `.env` automatically:

   ```bash
   set -a && source .env && set +a && ./mvnw spring-boot:run
   ```

4. Flyway runs on startup; Hibernate validates entities against the DB.
5. Optional catalog data: `scripts/seed/README.md`.

## Verification commands

```bash
./mvnw -q -DskipTests compile
./mvnw test
```

After schema or entity mapping changes, start the app once against Postgres so Flyway
and `validate` run. Compile alone is not enough.

## Related docs

- `AGENTS.md` — agent conventions and safety rules
- `docs/java-coding-conventions.md` — Java style and layering
- `docs/business-rules.md` — canonical product behavior
- `docs/development-status.md` — current state and next steps

Frontend (sibling repo `platform-ui`) consumes this API via `VITE_API_BASE_URL`.
