-- V1: initial LMS schema (13 tables)
-- UUID primary keys are supplied by the application; no DB UUID default/extension.

CREATE TABLE users (
    id              UUID            PRIMARY KEY,
    email           VARCHAR         NOT NULL,
    password_hash   VARCHAR         NOT NULL,
    full_name       VARCHAR         NOT NULL,
    role            VARCHAR         NOT NULL,
    avatar_url      VARCHAR,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('STUDENT', 'INSTRUCTOR', 'ADMIN'))
);

CREATE TABLE courses (
    id              UUID            PRIMARY KEY,
    title           VARCHAR         NOT NULL,
    description     TEXT,
    instructor_id   UUID            NOT NULL,
    price           NUMERIC         NOT NULL DEFAULT 0,
    is_published    BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_courses_instructor FOREIGN KEY (instructor_id) REFERENCES users (id),
    CONSTRAINT chk_courses_price CHECK (price >= 0)
);

CREATE INDEX idx_courses_instructor_id ON courses (instructor_id);

CREATE TABLE sections (
    id              UUID            PRIMARY KEY,
    title           VARCHAR         NOT NULL,
    course_id       UUID            NOT NULL,
    sort_order      INT             NOT NULL DEFAULT 0,
    CONSTRAINT fk_sections_course FOREIGN KEY (course_id) REFERENCES courses (id)
);

CREATE INDEX idx_sections_course_id ON sections (course_id);

CREATE TABLE lessons (
    id                  UUID            PRIMARY KEY,
    title               VARCHAR         NOT NULL,
    section_id          UUID            NOT NULL,
    lesson_type         VARCHAR         NOT NULL,
    content_url         TEXT,
    duration_seconds    INT,
    sort_order          INT             NOT NULL DEFAULT 0,
    CONSTRAINT fk_lessons_section FOREIGN KEY (section_id) REFERENCES sections (id),
    CONSTRAINT chk_lessons_lesson_type CHECK (lesson_type IN ('VIDEO', 'ARTICLE', 'QUIZ', 'ASSIGNMENT')),
    CONSTRAINT chk_lessons_duration_seconds CHECK (duration_seconds IS NULL OR duration_seconds >= 0)
);

CREATE INDEX idx_lessons_section_id ON lessons (section_id);

CREATE TABLE enrollments (
    id              UUID            PRIMARY KEY,
    user_id         UUID            NOT NULL,
    course_id       UUID            NOT NULL,
    status          VARCHAR         NOT NULL DEFAULT 'PENDING',
    enrolled_at     TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_enrollments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_enrollments_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT uq_enrollments_user_course UNIQUE (user_id, course_id),
    CONSTRAINT chk_enrollments_status CHECK (status IN ('PENDING', 'ACTIVE', 'COMPLETED', 'DROPPED'))
);

CREATE INDEX idx_enrollments_course_id ON enrollments (course_id);

CREATE TABLE lesson_progress (
    id                          UUID            PRIMARY KEY,
    user_id                     UUID            NOT NULL,
    lesson_id                   UUID            NOT NULL,
    is_completed                BOOLEAN         NOT NULL DEFAULT FALSE,
    watched_duration_seconds    INT             NOT NULL DEFAULT 0,
    last_accessed_at            TIMESTAMPTZ,
    CONSTRAINT fk_lesson_progress_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_lesson_progress_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id),
    CONSTRAINT uq_lesson_progress_user_lesson UNIQUE (user_id, lesson_id),
    CONSTRAINT chk_lesson_progress_watched_duration CHECK (watched_duration_seconds >= 0)
);

CREATE INDEX idx_lesson_progress_lesson_id ON lesson_progress (lesson_id);

CREATE TABLE quizzes (
    id                  UUID            PRIMARY KEY,
    lesson_id           UUID            NOT NULL,
    passing_score       INT             NOT NULL DEFAULT 70,
    time_limit_minutes  INT,
    max_attempts        INT,
    CONSTRAINT fk_quizzes_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id),
    CONSTRAINT uq_quizzes_lesson_id UNIQUE (lesson_id),
    CONSTRAINT chk_quizzes_passing_score CHECK (passing_score BETWEEN 0 AND 100),
    CONSTRAINT chk_quizzes_time_limit_minutes CHECK (time_limit_minutes IS NULL OR time_limit_minutes > 0),
    CONSTRAINT chk_quizzes_max_attempts CHECK (max_attempts IS NULL OR max_attempts > 0)
);

CREATE TABLE questions (
    id              UUID            PRIMARY KEY,
    quiz_id         UUID            NOT NULL,
    question_text   TEXT            NOT NULL,
    question_type   VARCHAR         NOT NULL,
    score_weight    INT             NOT NULL DEFAULT 1,
    CONSTRAINT fk_questions_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id),
    CONSTRAINT chk_questions_question_type CHECK (question_type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE')),
    CONSTRAINT chk_questions_score_weight CHECK (score_weight > 0)
);

CREATE INDEX idx_questions_quiz_id ON questions (quiz_id);

CREATE TABLE question_options (
    id              UUID            PRIMARY KEY,
    question_id     UUID            NOT NULL,
    option_text     TEXT            NOT NULL,
    is_correct      BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_question_options_question FOREIGN KEY (question_id) REFERENCES questions (id)
);

CREATE INDEX idx_question_options_question_id ON question_options (question_id);

CREATE TABLE quiz_attempts (
    id              UUID            PRIMARY KEY,
    user_id         UUID            NOT NULL,
    quiz_id         UUID            NOT NULL,
    score           NUMERIC,
    is_passed       BOOLEAN,
    started_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at    TIMESTAMPTZ,
    CONSTRAINT fk_quiz_attempts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_quiz_attempts_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id),
    CONSTRAINT chk_quiz_attempts_score CHECK (score IS NULL OR score >= 0)
);

CREATE INDEX idx_quiz_attempts_user_id ON quiz_attempts (user_id);
CREATE INDEX idx_quiz_attempts_quiz_id ON quiz_attempts (quiz_id);

CREATE TABLE quiz_attempt_answers (
    id              UUID            PRIMARY KEY,
    attempt_id      UUID            NOT NULL,
    question_id     UUID            NOT NULL,
    text_answer     TEXT,
    is_correct      BOOLEAN,
    CONSTRAINT fk_quiz_attempt_answers_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts (id),
    CONSTRAINT fk_quiz_attempt_answers_question FOREIGN KEY (question_id) REFERENCES questions (id),
    CONSTRAINT uq_quiz_attempt_answers_attempt_question UNIQUE (attempt_id, question_id)
);

CREATE INDEX idx_quiz_attempt_answers_question_id ON quiz_attempt_answers (question_id);

-- Selected options for SINGLE_CHOICE / MULTIPLE_CHOICE answers.
-- Application logic must ensure option_id belongs to the same question as
-- quiz_attempt_answers.question_id for the given attempt_answer_id; the
-- database cannot enforce that cross-table relationship.
CREATE TABLE quiz_attempt_answer_options (
    attempt_answer_id   UUID            NOT NULL,
    option_id           UUID            NOT NULL,
    CONSTRAINT pk_quiz_attempt_answer_options PRIMARY KEY (attempt_answer_id, option_id),
    CONSTRAINT fk_quiz_attempt_answer_options_answer FOREIGN KEY (attempt_answer_id) REFERENCES quiz_attempt_answers (id),
    CONSTRAINT fk_quiz_attempt_answer_options_option FOREIGN KEY (option_id) REFERENCES question_options (id)
);

CREATE INDEX idx_quiz_attempt_answer_options_option_id ON quiz_attempt_answer_options (option_id);

CREATE TABLE certificates (
    id                  UUID            PRIMARY KEY,
    user_id             UUID            NOT NULL,
    course_id           UUID            NOT NULL,
    certificate_code    VARCHAR         NOT NULL,
    pdf_url             VARCHAR,
    issued_at           TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_certificates_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_certificates_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT uq_certificates_code UNIQUE (certificate_code),
    CONSTRAINT uq_certificates_user_course UNIQUE (user_id, course_id)
);

CREATE INDEX idx_certificates_course_id ON certificates (course_id);
