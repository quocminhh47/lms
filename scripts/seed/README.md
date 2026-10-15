# Development course seed data

SQL seed for **local PostgreSQL** used by the LMS backend. Inserts demo **courses**, **sections**, and **lessons** only.

## What it inserts

| Item | Count / notes |
|------|----------------|
| Courses | 8 Chinese-learning topics (beginners, HSK 1/2, characters, travel Mandarin, grammar, business Chinese, culture) |
| Published | 7 courses (`is_published = true`) |
| Unpublished | 1 course — **Chinese Characters Made Practical** (draft) |
| Pricing | 6 free (`price = 0`); 2 paid examples (`49.99`, `79.00`) for UI/schema testing. Product rules treat MVP as free-first; payment is not implemented. |
| Sections | 26 total (2–4 per course), ordered by `sort_order` |
| Lessons | Chinese curriculum lessons with all four `lesson_type` values and mixed `duration_seconds` (including `NULL`) |

### Demo ID marker

All seeded rows use fixed UUIDs so reruns and cleanup stay scoped:

- **Courses:** `dec00001-0001-4001-8001-000000000001` … `000000000008`
- **Sections:** `dec00002-0001-4001-8001-…`
- **Lessons:** `dec00003-0001-4001-8001-…`

No `is_demo` column exists in the schema; identification is **by these UUID prefixes only**.

### Not seeded

Users, enrollments, lesson progress, quizzes, quiz attempts, certificates, and `courses.final_quiz_id` (left `NULL`).

## Preconditions

1. **Docker Compose Postgres** is running (`docker compose` service name: `postgres`, container: `lms-postgres`, database: `lms`, user: `lms_user`). Password comes from your local `.env` (`POSTGRES_PASSWORD`); do not commit it.
2. **Flyway** has applied at least `V1__initial_schema.sql` and `V2__assessment_lifecycle.sql` (normally by starting the Spring Boot app once against this database).
3. **At least one active user** with role `INSTRUCTOR` or `ADMIN` exists in `users`.  
   `courses.instructor_id` is `NOT NULL` and references `users`. The script picks the earliest `created_at` matching user.  
   Register via `POST /api/auth/register` (creates `STUDENT` only) **or** create an instructor/admin through your usual admin path before seeding.

## How to run (Docker Compose)

From the **backend repository root** (`lms/`):

```bash
docker compose up -d postgres
# Ensure migrations have run (e.g. start the app once), and an INSTRUCTOR/ADMIN user exists.

docker compose exec -T postgres psql -U lms_user -d lms -v ON_ERROR_STOP=1 -f - < scripts/seed/seed-courses.sql
```

Alternative (file mounted in container):

```bash
docker compose exec -T postgres psql -U lms_user -d lms -v ON_ERROR_STOP=1 -f /path/inside/container/seed-courses.sql
```

If you run `psql` on the host instead of `exec`, use the same database URL as the app (`localhost:5432`, database `lms`, user `lms_user`) and pass the password via `PGPASSWORD` from your environment — never paste passwords into this README.

## Verify inserted data

Run in `psql` (same connection as above):

```sql
-- Seeded course totals
SELECT
    COUNT(*) AS demo_courses,
    COUNT(*) FILTER (WHERE is_published) AS published,
    COUNT(*) FILTER (WHERE NOT is_published) AS unpublished
FROM courses
WHERE id::text LIKE 'dec00001-0001-4001-8001-%';

-- Sections per demo course
SELECT c.title, COUNT(s.id) AS sections
FROM courses c
LEFT JOIN sections s ON s.course_id = c.id
WHERE c.id::text LIKE 'dec00001-0001-4001-8001-%'
GROUP BY c.id, c.title
ORDER BY c.title;

-- Lessons per section (demo only)
SELECT c.title AS course, s.title AS section, COUNT(l.id) AS lessons
FROM courses c
JOIN sections s ON s.course_id = c.id
LEFT JOIN lessons l ON l.section_id = s.id
WHERE c.id::text LIKE 'dec00001-0001-4001-8001-%'
GROUP BY c.id, c.title, s.id, s.title, s.sort_order
ORDER BY c.title, s.sort_order;

-- Lesson type distribution (demo lessons)
SELECT l.lesson_type, COUNT(*)
FROM lessons l
JOIN sections s ON s.id = l.section_id
JOIN courses c ON c.id = s.course_id
WHERE c.id::text LIKE 'dec00001-0001-4001-8001-%'
GROUP BY l.lesson_type
ORDER BY l.lesson_type;

-- Orphan check (should return zero rows)
SELECT s.id AS orphan_section
FROM sections s
LEFT JOIN courses c ON c.id = s.course_id
WHERE s.id::text LIKE 'dec00002-0001-4001-8001-%' AND c.id IS NULL
UNION ALL
SELECT l.id
FROM lessons l
LEFT JOIN sections s ON s.id = l.section_id
WHERE l.id::text LIKE 'dec00003-0001-4001-8001-%' AND s.id IS NULL;
```

Public API check (with backend running): `GET /api/courses?published=true` should list seven demo courses; Spark should **not** appear for anonymous callers.

## Rerun behavior

The script runs in a **single transaction** (`BEGIN` … `COMMIT`).

On each run it **deletes only** demo lessons → demo sections → demo courses (by the fixed UUID lists), then re-inserts the same dataset. Other rows in the database are untouched.

If the instructor lookup fails, the transaction rolls back and nothing changes.

## Reset demo data only

Re-running `scripts/seed/seed-courses.sql` resets demo catalog content to the script’s definition.

To **remove** demo data without re-inserting, delete in FK order using the same ID lists as the script (lessons, then sections, then courses). Do not `TRUNCATE` catalog tables — that would remove non-demo data too.

If demo courses were referenced by **enrollments**, **lesson_progress**, **quizzes**, or **certificates**, deletes will fail until those dependent rows are removed. This seed does not create those links; the limitation matters only if you attached production-like data to demo course IDs manually.

## Assumptions and limitations

- **Instructor user** is not created by this script (out of scope). Seeding fails without an `INSTRUCTOR`/`ADMIN` user.
- **QUIZ** lessons are inserted without `quizzes` rows; quiz features in the app may expect separate quiz setup later.
- **content_url** values are placeholders (`https://example.dev/...`); they are not hosted media.
- **Paid** demo prices are for UI display only; business rules defer payment processing.
- Script was reviewed against `V1`/`V2` migrations; it was **not** executed against a live database as part of authoring.

Suggested commit message: `chore: add development course seed data`
