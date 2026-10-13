# LMS Business Rules

Canonical reference for LMS business behavior. Services and APIs must follow
this document. Schema notes reflect Flyway `V1` + `V2` and the current JPA model.

Status legend used below:

- **Confirmed** — established requirement for this project
- **Unresolved** — not yet decided; do not invent behavior in code
- **Schema/code mismatch** — requirement exists, but persistence or code does
  not fully encode or enforce it yet

---

## 1. User roles and access control

### Confirmed

- Roles: `STUDENT`, `INSTRUCTOR`, `ADMIN` (`UserRole`; DB check on `users.role`).
- Resource ownership and role-based authorization must be supported.
- Courses have an owning instructor (`courses.instructor_id`).
- Instructors may view detailed assessment results for students in courses
  they manage.
- Students must not see other students’ detailed answers.
- Public self-registration always creates a `STUDENT` account. Clients cannot
  assign `INSTRUCTOR` or `ADMIN` via registration.
- Authentication uses JWT bearer tokens (stateless). Passwords are stored
  only as BCrypt hashes.
- **Only `ADMIN` may create courses** (authorization foundation). Course
  `instructor_id` may still reference an `INSTRUCTOR` (or `ADMIN`) user as
  the course owner; the create API itself is ADMIN-only.

- User management authorization (implemented): create user, list users, and
  lookup by email are `ADMIN` only; a user may read or update only their own
  profile (name, avatar) and cannot change their own role or `active` flag;
  `ADMIN` may update any user's profile and `active` flag. No endpoint changes
  an existing user's role.

### Unresolved

- Exact permission matrix for remaining course mutations (update/unpublish
  ownership rules for instructor vs admin).
- Password policy beyond minimum length enforced at the API.
- Whether a user may hold only one role or multiple roles over time.
- Refresh tokens, password reset, and email verification (out of current scope).

### Schema / code mismatch

- Role changes and elevated account provisioning (e.g. creating an INSTRUCTOR)
  are not exposed via public APIs in the security foundation; use controlled
  admin processes when those features are added.

---

## 2. Course, Section, and Lesson management

### Confirmed

- A course contains ordered sections (`sections.sort_order`).
- A section contains ordered lessons (`lessons.sort_order`).
- Lesson types: `VIDEO`, `ARTICLE`, `QUIZ`, `ASSIGNMENT` (`LessonType`).
- MVP supports **free courses only** (`courses.price`; default `0`).
- Paid courses and payment processing are **deferred** (future).
- Full Assignment submission and grading are **not** part of the current MVP.
- Architecture should allow future learning-progress features
  (`lesson_progress` exists for that purpose).

### Unresolved

- Publishing workflow details beyond `courses.is_published`.
- Whether unpublished courses are visible to their instructor only, or also
  to admins.
- Whether lessons/sections have an explicit “required” flag, or all content
  counts toward completion by default.
- Unlock / sequential navigation rules between sections or lessons (beyond
  section-quiz 100% completion/unlocking noted in §7).

### Schema / code mismatch

- `ASSIGNMENT` exists as a lesson type, but there is no assignment-submission
  or grading schema.
- `courses.price` supports future paid courses; no payment tables exist
  (intentional deferral).

---

## 3. Enrollment lifecycle

### Confirmed

- A user may have **at most one enrollment per course**
  (`UNIQUE (user_id, course_id)`).
- Enrollment statuses: `PENDING`, `ACTIVE`, `COMPLETED`, `DROPPED`
  (`EnrollmentStatus`).
- Enrollment must control course access.
- For future paid courses: payment must succeed before paid-course enrollment
  becomes `ACTIVE` (not implemented in MVP).

### Unresolved

- Exact transition rules for MVP free courses (e.g. whether enrollment is
  created directly as `ACTIVE`, or always starts as `PENDING`).
- Who may drop an enrollment and whether `DROPPED` can re-enroll (unique
  constraint implies update-in-place or block re-create).
- When status moves to `COMPLETED` relative to certificate issuance.

### Schema / code mismatch

- Status values are stored; access-gating and transition logic are not
  implemented in services yet.

---

## 4. Ordinary quizzes vs Final Assessment

### Confirmed

- Each course has **at most one Final Assessment** in the MVP.
- A Final Assessment is modeled as a `Quiz`.
- Representation: `courses.final_quiz_id` → `quizzes.id` (Flyway `V2`).
- A quiz remains linked 1:1 to a lesson (`quizzes.lesson_id` unique).
- Section-level quizzes that gate completion/unlocking require a score of
  **100%**.
- Final Assessment passing score is **configurable**, defaulting to **80%**.
- Ordinary quiz passing thresholds may also be configurable.
- Failing an ordinary quiz does **not** automatically fail the entire course.
- Course completion requires completion of required learning content **and**
  passing the Final Assessment.

### Unresolved

- Precise definition of “required learning content” (all lessons vs subset).
- Whether every `QUIZ` lesson that is not the Final Assessment is a
  “section-level completion/unlock” quiz requiring 100%, or only some are.
- Whether a course may omit a Final Assessment (`final_quiz_id` null) and
  still be completable.

### Schema / code mismatch

- `quizzes.passing_score` has DB/Java default **70**, which does **not** match
  section (100) or Final Assessment (80) business defaults. Services **must**
  set `passing_score` explicitly on create; do not rely on the column default.
- The database does **not** guarantee that `courses.final_quiz_id` points to a
  quiz whose lesson belongs to that same course. Service must enforce:
  `finalQuiz.lesson.section.course.id == course.id`.
- Partial unique index ensures a quiz is final for at most one course; it does
  not enforce same-course membership.

---

## 5. Quiz attempt lifecycle, expiration, and attempt limits

### Confirmed

- Every valid attempt must preserve history (separate `quiz_attempts` rows;
  answers keyed by attempt).
- Attempt statuses: `IN_PROGRESS`, `SUBMITTED`, `EXPIRED`
  (`QuizAttemptStatus`; Flyway `V2`).
- Attempts keep `started_at`, `submitted_at`, `score`, `is_passed`, and
  optional `expires_at`.
- Expiration is determined using **server-side** timestamps.
- If an attempt expires before submission, it is marked `EXPIRED`; a new
  attempt is started when the student returns.
- Expired attempts are **not scored** and **do not consume** an allowed
  attempt.
- Students may retake quizzes and the Final Assessment after passing.
- `quizzes.max_attempts` may be null or a positive integer (DB check:
  null or `> 0`).

### Unresolved

- Exact meaning of `max_attempts = null` (treat as unlimited?).
- Whether at most one `IN_PROGRESS` attempt per user/quiz is required
  (recommended by schema design notes; not DB-enforced).
- How `expires_at` is derived when `time_limit_minutes` is null
  (no time limit?).

### Schema / code mismatch

- Columns/enums exist; lifecycle transitions and expiry jobs/checks are not
  implemented in services yet.
- No DB constraint preventing multiple concurrent `IN_PROGRESS` attempts.

---

## 6. Scoring and passing rules

### Confirmed

- Multiple-choice answers are correct only if the selected option set
  **exactly matches** the correct option set
  (`question_options.is_correct` + `quiz_attempt_answer_options`).
- Question types: `SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `TRUE_FALSE`
  (`QuestionType`).
- The **highest valid score** among attempts determines the assessment result.
- Valid scores come from **`SUBMITTED`** attempts only (not `EXPIRED`, and
  not unscored `IN_PROGRESS`).
- Passing compares that result to the quiz’s `passing_score`.
- Correct answers are **not shown** to students after submission.

### Unresolved

- Score scale details (0–100 vs weighted raw sum normalization).
- Whether `TRUE_FALSE` uses `question_options` the same way as choice
  questions, or `text_answer` (schema allows both paths).
- Rounding rules when comparing to `passing_score`.

### Schema / code mismatch

- The database cannot enforce that a selected `option_id` belongs to the same
  question as `quiz_attempt_answers.question_id`. Application logic must
  validate that (documented in `V1` migration).
- Hiding correct answers is an API/response concern; correctness flags remain
  stored for grading and instructor views.

---

## 7. Lesson / section / course completion

### Confirmed

- Per-user lesson progress is tracked in `lesson_progress`
  (`is_completed`, `watched_duration_seconds`, `last_accessed_at`).
- Section-level quiz completion/unlocking requires **100%** on that quiz
  (see §4).
- Course completion requires required learning content completed **and**
  Final Assessment passed.
- Ordinary quiz failure does not by itself fail the course.

### Unresolved

- Exact unlock graph (which content is locked until which quiz is passed).
- How VIDEO/ARTICLE completion is decided (watch threshold, manual mark,
  open-once, etc.).
- How QUIZ lesson `lesson_progress.is_completed` relates to best quiz score
  / `is_passed` (must be defined in services without inventing contradictory
  rules).
- Mapping from course completion to `enrollments.status = COMPLETED`.

### Schema / code mismatch

- Progress and enrollment status fields exist; completion orchestration is
  not implemented.

---

## 8. Certificates

### Confirmed

- A user may receive **at most one certificate per course**
  (`UNIQUE (user_id, course_id)`).
- Certificate codes must be **unique** (`certificate_code`).
- Certificate eligibility depends on course completion requirements (§7).

### Unresolved

- Certificate code generation format.
- Whether PDF generation (`pdf_url`) is required in MVP or deferred.
- Whether certificates can be revoked if enrollment is later dropped.

### Schema / code mismatch

- `certificates` table/entity/repository exist; issuance logic is not
  implemented.

---

## 9. Student vs instructor visibility of assessment results

### Confirmed

- After submission, students must **not** be shown correct answers.
- Students may see **aggregate** scores of other students in the **same
  course**, but **not** their detailed answers.
- Instructors may see **detailed** results for students in courses they
  manage (`courses.instructor_id`).

### Unresolved

- Definition of “aggregate” (class average only, anonymized distribution,
  etc.).
- Whether ADMIN has the same detailed visibility as the course instructor.
- Whether students see their own per-question correctness without revealing
  the correct option text/ids.

### Schema / code mismatch

- Visibility is not enforceable in the schema; it must be enforced in
  services and API response shaping (not implemented yet).

---

## 10. Persistence invariants for future services

These are confirmed operational rules tied to the current schema:

1. Assign application UUIDs; do not depend on DB UUID generation.
2. When setting `Course.finalQuiz`, verify the quiz belongs to that course’s
   lesson tree.
3. Set `Quiz.passingScore` explicitly on create; do not rely on default `70`.
4. Count attempt quota using non-`EXPIRED` attempts only (typically
   `SUBMITTED` + current `IN_PROGRESS` policy once decided).
5. Compute best score from `SUBMITTED` attempts only.
6. Never delete historical attempts to “reset” a quiz.
7. Validate selected options belong to the answered question.
8. Do not implement payment or assignment-submission flows in the current MVP
   scope.
