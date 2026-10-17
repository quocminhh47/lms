# LMS backend — development status

Snapshot for continuing work on a new machine. **Code and migrations are the source of truth.**
Last verified in repo: `./mvnw test` → **125 tests**, 0 failures (local run).

## Git (informational)

- Typical feature branch: `feat/lesson-progess-impl` (tracks `origin`).
- Check `git status` before assuming a clean tree; documentation edits may be uncommitted.

## Current project status

MVP monolith with **auth**, **published course catalog**, **ADMIN-managed curriculum**,
**free-course enrollment**, **enrollment-gated lesson content**, and **lesson progress**
for enrolled learners. Assessment attempts, course completion orchestration, certificates,
and paid enrollment are **not** implemented.

## Recently completed (on feature branches)

- **V4 user profile**: `first_name`, `last_name`, unique normalized phone; registration and profile APIs updated.
- **Login**: email or phone via `identifier`; legacy `email` JSON alias on `LoginRequest`.
- **Enrollment**: `POST /api/courses/{courseId}/enrollments`, `GET /api/enrollments/me` (+ by id); free published courses only → `ACTIVE`.
- **Lesson progress**: course-wide and per-lesson GET/PUT; POST complete for VIDEO/ARTICLE only.
- **Integration tests**: auth registration/login, security, enrollment, progress services.

## Implemented vs not implemented

| Implemented | Not implemented (schema may exist) |
|-------------|-------------------------------------|
| JWT register/login/me | Refresh tokens, password reset, email verify |
| User CRUD (rules in business-rules §1) | Public role elevation |
| Course/section/lesson CRUD + publish (ADMIN) | Instructor-owned course admin APIs |
| Public catalog + curriculum metadata | Payment / paid enroll |
| Lesson `contentUrl` gating by enrollment | Assignment submission |
| Free enroll + my enrollments | Quiz start/submit/scoring APIs |
| Lesson progress APIs | Auto course COMPLETED / certificates |
| Seed SQL for demo courses | — |

## Known issues and operational notes

- **Stale JVM on port 8080**: After pulling auth or progress changes, restart
  `./mvnw spring-boot:run`. An old process serves outdated DTOs (e.g. login 400
  `"email must not be blank"` when the UI sends only `identifier`).
- **V4 migration**: Fails if existing rows produce duplicate phones after normalization.
- **CORS**: Dev UI must use an origin listed in `CORS_ALLOWED_ORIGINS` (default includes `5173`, not necessarily other Vite ports).
- **Seed data**: Two courses have non-zero `price`; MVP enroll rejects paid courses with 400.

## Important decisions (constraints)

- Monolith, **technical-layer** packages under `com.lms` — do not introduce feature root packages.
- `docs/business-rules.md` is canonical; do not invent rules for **Unresolved** items.
- **Do not edit applied Flyway migrations**; add new `Vn__` files only.
- Paid enrollment, payments, assignment grading deferred unless explicitly requested.

## Next recommended tasks

1. **Quiz attempt API** — start/submit flow using `V2` schema; align `passing_score` with §4 (do not rely on DB default 70).
2. **Course completion** — derive required content + Final Assessment; optionally set `enrollments.status = COMPLETED`.
3. **Instructor endpoints** (if product confirms) — manage owned courses without full ADMIN.

Dependencies: stable auth + enroll + progress (done); frontend learning workspace already calls progress APIs.

## Environment variables (names only)

From `.env.example`: `POSTGRES_PASSWORD`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
`JWT_SECRET`, `JWT_EXPIRATION_MS`, `CORS_ALLOWED_ORIGINS`.

## Tests and build

```bash
./mvnw -q -DskipTests compile
./mvnw test
```

No separate linter configured for Java in this repo; rely on tests and compile.
