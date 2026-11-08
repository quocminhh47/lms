-- Preserve stable question/option ordering for admin quiz configuration API.

ALTER TABLE questions
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0;

ALTER TABLE question_options
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0;

CREATE INDEX idx_questions_quiz_sort_order ON questions (quiz_id, sort_order);
CREATE INDEX idx_question_options_question_sort_order ON question_options (question_id, sort_order);
