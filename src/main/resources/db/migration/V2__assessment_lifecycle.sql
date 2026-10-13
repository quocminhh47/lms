-- V2: assessment lifecycle
--
-- Final Assessment representation:
--   courses.final_quiz_id -> quizzes.id
--   A course may reference at most one Final Assessment Quiz.
--   The DB does NOT enforce that the referenced Quiz belongs to the same
--   Course's section/lesson tree; the Service layer must validate:
--     quiz.lesson.section.course_id = course.id
--
-- Passing-score policy:
--   quizzes.passing_score remains INT NOT NULL with CHECK 0..100.
--   Do NOT rely on the column DEFAULT (70) for business rules.
--   The Service must set passing_score explicitly when creating a Quiz:
--     - section-level Quiz completion/unlock requirement: 100
--     - Final Assessment: configurable, typically default 80
--     - other ordinary Quizzes: configurable as needed

-- ---------------------------------------------------------------------------
-- Final Assessment pointer on courses
-- ---------------------------------------------------------------------------
ALTER TABLE courses
    ADD COLUMN final_quiz_id UUID NULL;

ALTER TABLE courses
    ADD CONSTRAINT fk_courses_final_quiz
        FOREIGN KEY (final_quiz_id) REFERENCES quizzes (id);

-- A given Quiz may be designated as Final Assessment for at most one Course.
CREATE UNIQUE INDEX uq_courses_final_quiz_id
    ON courses (final_quiz_id)
    WHERE final_quiz_id IS NOT NULL;

CREATE INDEX idx_courses_final_quiz_id ON courses (final_quiz_id);

-- ---------------------------------------------------------------------------
-- QuizAttempt lifecycle: status + expires_at
-- ---------------------------------------------------------------------------
ALTER TABLE quiz_attempts
    ADD COLUMN status VARCHAR NULL;

ALTER TABLE quiz_attempts
    ADD COLUMN expires_at TIMESTAMPTZ NULL;

-- Safe backfill for existing rows:
--   - submitted_at present  => SUBMITTED (never mark as IN_PROGRESS)
--   - otherwise             => IN_PROGRESS
-- Historical rows are not reclassified as EXPIRED (expiry is a runtime concern).
UPDATE quiz_attempts
SET status = CASE
                 WHEN submitted_at IS NOT NULL THEN 'SUBMITTED'
                 ELSE 'IN_PROGRESS'
             END
WHERE status IS NULL;

ALTER TABLE quiz_attempts
    ALTER COLUMN status SET DEFAULT 'IN_PROGRESS';

ALTER TABLE quiz_attempts
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE quiz_attempts
    ADD CONSTRAINT chk_quiz_attempts_status
        CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'EXPIRED'));

CREATE INDEX idx_quiz_attempts_user_quiz_status
    ON quiz_attempts (user_id, quiz_id, status);
