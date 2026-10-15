-- Development seed: Chinese-learning demo courses, sections, and lessons.
-- Marker: demo IDs use prefixes dec00001/02/03 with valid UUIDs (12 hex digits in the last segment).
-- Does not seed users, enrollments, quizzes, or progress (except clearing dependents on reset).
--
-- Preconditions:
--   - Flyway migrations V1 and V2 applied.
--   - At least one active users row with role INSTRUCTOR or ADMIN (instructor_id FK).

\set ON_ERROR_STOP on

BEGIN;

-- ---------------------------------------------------------------------------
-- Demo course IDs (stable). Reruns replace only this set.
-- ---------------------------------------------------------------------------
-- Courses: dec00001-0001-4001-8001-000000000001 .. 000000000008

-- ---------------------------------------------------------------------------
-- Remove previous demo rows (dependents first). Only touches demo ID sets.
-- ---------------------------------------------------------------------------
DELETE FROM quiz_attempt_answer_options
WHERE attempt_answer_id IN (
    SELECT aa.id
    FROM quiz_attempt_answers aa
    JOIN quiz_attempts qa ON qa.id = aa.attempt_id
    JOIN quizzes q ON q.id = qa.quiz_id
    JOIN lessons l ON l.id = q.lesson_id
    JOIN sections s ON s.id = l.section_id
    WHERE s.course_id IN (
        'dec00001-0001-4001-8001-000000000001'::uuid,
        'dec00001-0001-4001-8001-000000000002'::uuid,
        'dec00001-0001-4001-8001-000000000003'::uuid,
        'dec00001-0001-4001-8001-000000000004'::uuid,
        'dec00001-0001-4001-8001-000000000005'::uuid,
        'dec00001-0001-4001-8001-000000000006'::uuid,
        'dec00001-0001-4001-8001-000000000007'::uuid,
        'dec00001-0001-4001-8001-000000000008'::uuid
    )
);

DELETE FROM quiz_attempt_answers
WHERE attempt_id IN (
    SELECT qa.id
    FROM quiz_attempts qa
    JOIN quizzes q ON q.id = qa.quiz_id
    JOIN lessons l ON l.id = q.lesson_id
    JOIN sections s ON s.id = l.section_id
    WHERE s.course_id IN (
        'dec00001-0001-4001-8001-000000000001'::uuid,
        'dec00001-0001-4001-8001-000000000002'::uuid,
        'dec00001-0001-4001-8001-000000000003'::uuid,
        'dec00001-0001-4001-8001-000000000004'::uuid,
        'dec00001-0001-4001-8001-000000000005'::uuid,
        'dec00001-0001-4001-8001-000000000006'::uuid,
        'dec00001-0001-4001-8001-000000000007'::uuid,
        'dec00001-0001-4001-8001-000000000008'::uuid
    )
);

DELETE FROM quiz_attempts
WHERE quiz_id IN (
    SELECT q.id
    FROM quizzes q
    JOIN lessons l ON l.id = q.lesson_id
    JOIN sections s ON s.id = l.section_id
    WHERE s.course_id IN (
        'dec00001-0001-4001-8001-000000000001'::uuid,
        'dec00001-0001-4001-8001-000000000002'::uuid,
        'dec00001-0001-4001-8001-000000000003'::uuid,
        'dec00001-0001-4001-8001-000000000004'::uuid,
        'dec00001-0001-4001-8001-000000000005'::uuid,
        'dec00001-0001-4001-8001-000000000006'::uuid,
        'dec00001-0001-4001-8001-000000000007'::uuid,
        'dec00001-0001-4001-8001-000000000008'::uuid
    )
);

DELETE FROM question_options
WHERE question_id IN (
    SELECT qq.id
    FROM questions qq
    JOIN quizzes q ON q.id = qq.quiz_id
    JOIN lessons l ON l.id = q.lesson_id
    JOIN sections s ON s.id = l.section_id
    WHERE s.course_id IN (
        'dec00001-0001-4001-8001-000000000001'::uuid,
        'dec00001-0001-4001-8001-000000000002'::uuid,
        'dec00001-0001-4001-8001-000000000003'::uuid,
        'dec00001-0001-4001-8001-000000000004'::uuid,
        'dec00001-0001-4001-8001-000000000005'::uuid,
        'dec00001-0001-4001-8001-000000000006'::uuid,
        'dec00001-0001-4001-8001-000000000007'::uuid,
        'dec00001-0001-4001-8001-000000000008'::uuid
    )
);

DELETE FROM questions
WHERE quiz_id IN (
    SELECT q.id
    FROM quizzes q
    JOIN lessons l ON l.id = q.lesson_id
    JOIN sections s ON s.id = l.section_id
    WHERE s.course_id IN (
        'dec00001-0001-4001-8001-000000000001'::uuid,
        'dec00001-0001-4001-8001-000000000002'::uuid,
        'dec00001-0001-4001-8001-000000000003'::uuid,
        'dec00001-0001-4001-8001-000000000004'::uuid,
        'dec00001-0001-4001-8001-000000000005'::uuid,
        'dec00001-0001-4001-8001-000000000006'::uuid,
        'dec00001-0001-4001-8001-000000000007'::uuid,
        'dec00001-0001-4001-8001-000000000008'::uuid
    )
);

UPDATE courses
SET final_quiz_id = NULL
WHERE id IN (
    'dec00001-0001-4001-8001-000000000001'::uuid,
    'dec00001-0001-4001-8001-000000000002'::uuid,
    'dec00001-0001-4001-8001-000000000003'::uuid,
    'dec00001-0001-4001-8001-000000000004'::uuid,
    'dec00001-0001-4001-8001-000000000005'::uuid,
    'dec00001-0001-4001-8001-000000000006'::uuid,
    'dec00001-0001-4001-8001-000000000007'::uuid,
    'dec00001-0001-4001-8001-000000000008'::uuid
);

DELETE FROM quizzes
WHERE lesson_id IN (
    SELECT l.id
    FROM lessons l
    JOIN sections s ON s.id = l.section_id
    WHERE s.course_id IN (
        'dec00001-0001-4001-8001-000000000001'::uuid,
        'dec00001-0001-4001-8001-000000000002'::uuid,
        'dec00001-0001-4001-8001-000000000003'::uuid,
        'dec00001-0001-4001-8001-000000000004'::uuid,
        'dec00001-0001-4001-8001-000000000005'::uuid,
        'dec00001-0001-4001-8001-000000000006'::uuid,
        'dec00001-0001-4001-8001-000000000007'::uuid,
        'dec00001-0001-4001-8001-000000000008'::uuid
    )
);

DELETE FROM lesson_progress
WHERE lesson_id IN (
    SELECT l.id
    FROM lessons l
    JOIN sections s ON s.id = l.section_id
    WHERE s.course_id IN (
        'dec00001-0001-4001-8001-000000000001'::uuid,
        'dec00001-0001-4001-8001-000000000002'::uuid,
        'dec00001-0001-4001-8001-000000000003'::uuid,
        'dec00001-0001-4001-8001-000000000004'::uuid,
        'dec00001-0001-4001-8001-000000000005'::uuid,
        'dec00001-0001-4001-8001-000000000006'::uuid,
        'dec00001-0001-4001-8001-000000000007'::uuid,
        'dec00001-0001-4001-8001-000000000008'::uuid
    )
);

DELETE FROM certificates
WHERE course_id IN (
    'dec00001-0001-4001-8001-000000000001'::uuid,
    'dec00001-0001-4001-8001-000000000002'::uuid,
    'dec00001-0001-4001-8001-000000000003'::uuid,
    'dec00001-0001-4001-8001-000000000004'::uuid,
    'dec00001-0001-4001-8001-000000000005'::uuid,
    'dec00001-0001-4001-8001-000000000006'::uuid,
    'dec00001-0001-4001-8001-000000000007'::uuid,
    'dec00001-0001-4001-8001-000000000008'::uuid
);

DELETE FROM enrollments
WHERE course_id IN (
    'dec00001-0001-4001-8001-000000000001'::uuid,
    'dec00001-0001-4001-8001-000000000002'::uuid,
    'dec00001-0001-4001-8001-000000000003'::uuid,
    'dec00001-0001-4001-8001-000000000004'::uuid,
    'dec00001-0001-4001-8001-000000000005'::uuid,
    'dec00001-0001-4001-8001-000000000006'::uuid,
    'dec00001-0001-4001-8001-000000000007'::uuid,
    'dec00001-0001-4001-8001-000000000008'::uuid
);

DELETE FROM lessons
WHERE section_id IN (
    SELECT s.id
    FROM sections s
    WHERE s.course_id IN (
        'dec00001-0001-4001-8001-000000000001'::uuid,
        'dec00001-0001-4001-8001-000000000002'::uuid,
        'dec00001-0001-4001-8001-000000000003'::uuid,
        'dec00001-0001-4001-8001-000000000004'::uuid,
        'dec00001-0001-4001-8001-000000000005'::uuid,
        'dec00001-0001-4001-8001-000000000006'::uuid,
        'dec00001-0001-4001-8001-000000000007'::uuid,
        'dec00001-0001-4001-8001-000000000008'::uuid
    )
);

DELETE FROM sections
WHERE course_id IN (
    'dec00001-0001-4001-8001-000000000001'::uuid,
    'dec00001-0001-4001-8001-000000000002'::uuid,
    'dec00001-0001-4001-8001-000000000003'::uuid,
    'dec00001-0001-4001-8001-000000000004'::uuid,
    'dec00001-0001-4001-8001-000000000005'::uuid,
    'dec00001-0001-4001-8001-000000000006'::uuid,
    'dec00001-0001-4001-8001-000000000007'::uuid,
    'dec00001-0001-4001-8001-000000000008'::uuid
);

DELETE FROM courses
WHERE id IN (
    'dec00001-0001-4001-8001-000000000001'::uuid,
    'dec00001-0001-4001-8001-000000000002'::uuid,
    'dec00001-0001-4001-8001-000000000003'::uuid,
    'dec00001-0001-4001-8001-000000000004'::uuid,
    'dec00001-0001-4001-8001-000000000005'::uuid,
    'dec00001-0001-4001-8001-000000000006'::uuid,
    'dec00001-0001-4001-8001-000000000007'::uuid,
    'dec00001-0001-4001-8001-000000000008'::uuid
);

-- ---------------------------------------------------------------------------
-- Resolve instructor (required FK). Fails fast if no suitable user exists.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_instructor_id UUID;
BEGIN
    SELECT u.id
    INTO v_instructor_id
    FROM users u
    WHERE u.role IN ('INSTRUCTOR', 'ADMIN')
      AND u.is_active = TRUE
    ORDER BY u.created_at
    LIMIT 1;

    IF v_instructor_id IS NULL THEN
        RAISE EXCEPTION
            'seed-courses.sql: no active INSTRUCTOR or ADMIN user found in users. Create one before seeding.';
    END IF;

    PERFORM set_config('seed.instructor_id', v_instructor_id::text, true);
END $$;

-- ---------------------------------------------------------------------------
-- Courses (8 Chinese-learning topics): 7 published, 1 unpublished draft
-- ---------------------------------------------------------------------------
INSERT INTO courses (
    id, title, description, instructor_id, price, is_published,
    created_at, updated_at, final_quiz_id
) VALUES
(
    'dec00001-0001-4001-8001-000000000001',
    'Chinese for Absolute Beginners: Pinyin, Tones, and First Conversations',
    'Start Mandarin from zero. Learn Pinyin, the four tones, greetings, and simple classroom phrases with guided listening and speaking practice.',
    current_setting('seed.instructor_id')::uuid,
    0,
    TRUE,
    '2026-01-15T10:00:00+00',
    '2026-01-15T10:00:00+00',
    NULL
),
(
    'dec00001-0001-4001-8001-000000000002',
    'HSK 1 Vocabulary and Grammar Essentials',
    'Build the HSK 1 word list and core sentence patterns: introductions, numbers, time, food, and daily routines. Includes short reading drills.',
    current_setting('seed.instructor_id')::uuid,
    0,
    TRUE,
    '2026-01-15T10:05:00+00',
    '2026-01-15T10:05:00+00',
    NULL
),
(
    'dec00001-0001-4001-8001-000000000003',
    'Chinese Characters Made Practical: Stroke Order and Radicals',
    'Draft course for recognizing radicals, writing stroke order correctly, and memorizing high-frequency characters. Unpublished while sample worksheets are finished.',
    current_setting('seed.instructor_id')::uuid,
    0,
    FALSE,
    '2026-01-15T10:10:00+00',
    '2026-01-15T10:10:00+00',
    NULL
),
(
    'dec00001-0001-4001-8001-000000000004',
    'Everyday Mandarin Conversations for Travel and Daily Life',
    'Practice ordering food, asking for directions, shopping, hotels, and small talk. Focus on listening comprehension and natural reply patterns.',
    current_setting('seed.instructor_id')::uuid,
    0,
    TRUE,
    '2026-01-15T10:15:00+00',
    '2026-01-15T10:15:00+00',
    NULL
),
(
    'dec00001-0001-4001-8001-000000000005',
    'HSK 2 Listening and Speaking Bootcamp',
    'Move past HSK 1 with longer dialogues, question words, comparisons, and describing past events. Ideal for learners aiming at HSK 2 oral fluency.',
    current_setting('seed.instructor_id')::uuid,
    49.99,
    TRUE,
    '2026-01-15T10:20:00+00',
    '2026-01-15T10:20:00+00',
    NULL
),
(
    'dec00001-0001-4001-8001-000000000006',
    'Chinese Grammar Patterns for Intermediate Learners',
    'Master ba/bei sentences, complements of direction and result, aspect particles, and common connectors used in short essays and spoken Mandarin.',
    current_setting('seed.instructor_id')::uuid,
    79.00,
    TRUE,
    '2026-01-15T10:25:00+00',
    '2026-01-15T10:25:00+00',
    NULL
),
(
    'dec00001-0001-4001-8001-000000000007',
    'Business Chinese Basics: Meetings, Emails, and Polite Requests',
    'Learn workplace greetings, scheduling language, polite requests, and simple email openings. Designed for professionals starting Chinese at work.',
    current_setting('seed.instructor_id')::uuid,
    0,
    TRUE,
    '2026-01-15T10:30:00+00',
    '2026-01-15T10:30:00+00',
    NULL
),
(
    'dec00001-0001-4001-8001-000000000008',
    'Chinese Culture through Language: Festivals, Food, and Etiquette',
    'Connect language learning with Mid-Autumn, Spring Festival, dining etiquette, and gift-giving phrases. Mix of reading, listening, and short writing tasks.',
    current_setting('seed.instructor_id')::uuid,
    0,
    TRUE,
    '2026-01-15T10:35:00+00',
    '2026-01-15T10:35:00+00',
    NULL
);

-- ---------------------------------------------------------------------------
-- Sections
-- ---------------------------------------------------------------------------
INSERT INTO sections (id, title, course_id, sort_order) VALUES
-- Absolute beginners (3)
('dec00002-0001-4001-8001-000000010101', 'Sounds and tones', 'dec00001-0001-4001-8001-000000000001', 0),
('dec00002-0001-4001-8001-000000010102', 'First words and greetings', 'dec00001-0001-4001-8001-000000000001', 1),
('dec00002-0001-4001-8001-000000010103', 'Classroom Chinese', 'dec00001-0001-4001-8001-000000000001', 2),
-- HSK 1 (4)
('dec00002-0001-4001-8001-000000020101', 'People and introductions', 'dec00001-0001-4001-8001-000000000002', 0),
('dec00002-0001-4001-8001-000000020102', 'Numbers, time, and dates', 'dec00001-0001-4001-8001-000000000002', 1),
('dec00002-0001-4001-8001-000000020103', 'Food and daily life', 'dec00001-0001-4001-8001-000000000002', 2),
('dec00002-0001-4001-8001-000000020104', 'HSK 1 practice set', 'dec00001-0001-4001-8001-000000000002', 3),
-- Characters draft (2)
('dec00002-0001-4001-8001-000000030101', 'Stroke order foundations', 'dec00001-0001-4001-8001-000000000003', 0),
('dec00002-0001-4001-8001-000000030102', 'Radicals and recognition', 'dec00001-0001-4001-8001-000000000003', 1),
-- Travel conversations (3)
('dec00002-0001-4001-8001-000000040101', 'Getting around', 'dec00001-0001-4001-8001-000000000004', 0),
('dec00002-0001-4001-8001-000000040102', 'Restaurants and shopping', 'dec00001-0001-4001-8001-000000000004', 1),
('dec00002-0001-4001-8001-000000040103', 'Hotels and help', 'dec00001-0001-4001-8001-000000000004', 2),
-- HSK 2 bootcamp (3)
('dec00002-0001-4001-8001-000000050101', 'Longer listening', 'dec00001-0001-4001-8001-000000000005', 0),
('dec00002-0001-4001-8001-000000050102', 'Speaking patterns', 'dec00001-0001-4001-8001-000000000005', 1),
('dec00002-0001-4001-8001-000000050103', 'HSK 2 checkpoints', 'dec00001-0001-4001-8001-000000000005', 2),
-- Grammar intermediate (4)
('dec00002-0001-4001-8001-000000060101', 'Aspect and particles', 'dec00001-0001-4001-8001-000000000006', 0),
('dec00002-0001-4001-8001-000000060102', 'Ba and bei', 'dec00001-0001-4001-8001-000000000006', 1),
('dec00002-0001-4001-8001-000000060103', 'Complements', 'dec00001-0001-4001-8001-000000000006', 2),
('dec00002-0001-4001-8001-000000060104', 'Connectors for writing', 'dec00001-0001-4001-8001-000000000006', 3),
-- Business Chinese (3)
('dec00002-0001-4001-8001-000000070101', 'Workplace greetings', 'dec00001-0001-4001-8001-000000000007', 0),
('dec00002-0001-4001-8001-000000070102', 'Meetings and schedules', 'dec00001-0001-4001-8001-000000000007', 1),
('dec00002-0001-4001-8001-000000070103', 'Email and polite requests', 'dec00001-0001-4001-8001-000000000007', 2),
-- Culture (4)
('dec00002-0001-4001-8001-000000080101', 'Festivals', 'dec00001-0001-4001-8001-000000000008', 0),
('dec00002-0001-4001-8001-000000080102', 'Food culture', 'dec00001-0001-4001-8001-000000000008', 1),
('dec00002-0001-4001-8001-000000080103', 'Etiquette phrases', 'dec00001-0001-4001-8001-000000000008', 2),
('dec00002-0001-4001-8001-000000080104', 'Short cultural readings', 'dec00001-0001-4001-8001-000000000008', 3);

-- ---------------------------------------------------------------------------
-- Lessons
-- ---------------------------------------------------------------------------
INSERT INTO lessons (
    id, title, section_id, lesson_type, content_url, duration_seconds, sort_order
) VALUES
-- Beginners §1
('dec00003-0001-4001-8001-000001010101', 'What is Pinyin?', 'dec00002-0001-4001-8001-000000010101', 'VIDEO', 'https://example.dev/lms/zh/pinyin-intro', 480, 0),
('dec00003-0001-4001-8001-000001010102', 'Tone chart reference', 'dec00002-0001-4001-8001-000000010101', 'ARTICLE', 'https://example.dev/lms/zh/tones', NULL, 1),
('dec00003-0001-4001-8001-000001010103', 'Tone discrimination quiz', 'dec00002-0001-4001-8001-000000010101', 'QUIZ', NULL, NULL, 2),
-- Beginners §2
('dec00003-0001-4001-8001-000001010201', 'Hello, thank you, goodbye', 'dec00002-0001-4001-8001-000000010102', 'VIDEO', 'https://example.dev/lms/zh/greetings', 600, 0),
('dec00003-0001-4001-8001-000001010202', 'Greeting phrase sheet', 'dec00002-0001-4001-8001-000000010102', 'ARTICLE', 'https://example.dev/lms/zh/greeting-sheet', 300, 1),
('dec00003-0001-4001-8001-000001010203', 'Record three greetings', 'dec00002-0001-4001-8001-000000010102', 'ASSIGNMENT', 'https://example.dev/lms/zh/greeting-lab', NULL, 2),
-- Beginners §3
('dec00003-0001-4001-8001-000001010301', 'Please repeat and speak slowly', 'dec00002-0001-4001-8001-000000010103', 'VIDEO', 'https://example.dev/lms/zh/classroom', 420, 0),
('dec00003-0001-4001-8001-000001010302', 'Classroom Chinese quiz', 'dec00002-0001-4001-8001-000000010103', 'QUIZ', NULL, 360, 1),
-- HSK1 §1
('dec00003-0001-4001-8001-000002010101', 'I am … / This is …', 'dec00002-0001-4001-8001-000000020101', 'VIDEO', 'https://example.dev/lms/zh/hsk1-intro', 540, 0),
('dec00003-0001-4001-8001-000002010102', 'Family and nationality words', 'dec00002-0001-4001-8001-000000020101', 'ARTICLE', 'https://example.dev/lms/zh/hsk1-people', NULL, 1),
-- HSK1 §2
('dec00003-0001-4001-8001-000002010201', 'Counting to 100', 'dec00002-0001-4001-8001-000000020102', 'VIDEO', 'https://example.dev/lms/zh/numbers', 720, 0),
('dec00003-0001-4001-8001-000002010202', 'Asking the time', 'dec00002-0001-4001-8001-000000020102', 'ARTICLE', 'https://example.dev/lms/zh/time', 480, 1),
('dec00003-0001-4001-8001-000002010203', 'Write five time sentences', 'dec00002-0001-4001-8001-000000020102', 'ASSIGNMENT', NULL, NULL, 2),
-- HSK1 §3
('dec00003-0001-4001-8001-000002010301', 'Ordering simple meals', 'dec00002-0001-4001-8001-000000020103', 'VIDEO', 'https://example.dev/lms/zh/food', 660, 0),
('dec00003-0001-4001-8001-000002010302', 'Daily routine verbs', 'dec00002-0001-4001-8001-000000020103', 'ARTICLE', 'https://example.dev/lms/zh/routine', 540, 1),
-- HSK1 §4
('dec00003-0001-4001-8001-000002010401', 'Mini reading: my weekend', 'dec00002-0001-4001-8001-000000020104', 'ARTICLE', 'https://example.dev/lms/zh/hsk1-reading', NULL, 0),
('dec00003-0001-4001-8001-000002010402', 'HSK 1 review quiz', 'dec00002-0001-4001-8001-000000020104', 'QUIZ', NULL, 900, 1),
-- Characters draft §1
('dec00003-0001-4001-8001-000003010101', 'Basic strokes overview', 'dec00002-0001-4001-8001-000000030101', 'ARTICLE', 'https://example.dev/lms/zh/strokes', NULL, 0),
('dec00003-0001-4001-8001-000003010102', 'Stroke order quiz', 'dec00002-0001-4001-8001-000000030101', 'QUIZ', NULL, 300, 1),
-- Characters draft §2
('dec00003-0001-4001-8001-000003010201', 'Common radicals', 'dec00002-0001-4001-8001-000000030102', 'VIDEO', 'https://example.dev/lms/zh/radicals', 780, 0),
('dec00003-0001-4001-8001-000003010202', 'Radical recognition drill', 'dec00002-0001-4001-8001-000000030102', 'ARTICLE', 'https://example.dev/lms/zh/radical-drill', 420, 1),
-- Travel §1
('dec00003-0001-4001-8001-000004010101', 'Where is the subway?', 'dec00002-0001-4001-8001-000000040101', 'VIDEO', 'https://example.dev/lms/zh/directions', 600, 0),
('dec00003-0001-4001-8001-000004010102', 'Direction phrase sheet', 'dec00002-0001-4001-8001-000000040101', 'ARTICLE', 'https://example.dev/lms/zh/direction-sheet', NULL, 1),
-- Travel §2
('dec00003-0001-4001-8001-000004010201', 'Ordering at a restaurant', 'dec00002-0001-4001-8001-000000040102', 'VIDEO', 'https://example.dev/lms/zh/restaurant', 540, 0),
('dec00003-0001-4001-8001-000004010202', 'Shopping dialogue practice', 'dec00002-0001-4001-8001-000000040102', 'ASSIGNMENT', 'https://example.dev/lms/zh/shopping-lab', NULL, 1),
('dec00003-0001-4001-8001-000004010203', 'Market listening quiz', 'dec00002-0001-4001-8001-000000040102', 'QUIZ', NULL, 420, 2),
-- Travel §3
('dec00003-0001-4001-8001-000004010301', 'Hotel check-in phrases', 'dec00002-0001-4001-8001-000000040103', 'ARTICLE', 'https://example.dev/lms/zh/hotel', 360, 0),
-- HSK2 §1
('dec00003-0001-4001-8001-000005010101', 'Listening: weekend plans', 'dec00002-0001-4001-8001-000000050101', 'VIDEO', 'https://example.dev/lms/zh/hsk2-listen', 720, 0),
('dec00003-0001-4001-8001-000005010102', 'Listening transcript notes', 'dec00002-0001-4001-8001-000000050101', 'ARTICLE', 'https://example.dev/lms/zh/hsk2-transcript', NULL, 1),
('dec00003-0001-4001-8001-000005010103', 'Listening comprehension quiz', 'dec00002-0001-4001-8001-000000050101', 'QUIZ', NULL, 480, 2),
('dec00003-0001-4001-8001-000005010104', 'Shadow the dialogue', 'dec00002-0001-4001-8001-000000050101', 'ASSIGNMENT', 'https://example.dev/lms/zh/hsk2-shadow', 600, 3),
-- HSK2 §2
('dec00003-0001-4001-8001-000005010201', 'Comparisons and preferences', 'dec00002-0001-4001-8001-000000050102', 'VIDEO', 'https://example.dev/lms/zh/compare', 660, 0),
('dec00003-0001-4001-8001-000005010202', 'Talk about yesterday', 'dec00002-0001-4001-8001-000000050102', 'ASSIGNMENT', NULL, NULL, 1),
('dec00003-0001-4001-8001-000005010203', 'Speaking pattern quiz', 'dec00002-0001-4001-8001-000000050102', 'QUIZ', NULL, 420, 2),
-- HSK2 §3
('dec00003-0001-4001-8001-000005010301', 'HSK 2 mock listening', 'dec00002-0001-4001-8001-000000050103', 'VIDEO', 'https://example.dev/lms/zh/hsk2-mock', 900, 0),
('dec00003-0001-4001-8001-000005010302', 'Checkpoint quiz', 'dec00002-0001-4001-8001-000000050103', 'QUIZ', NULL, 720, 1),
-- Grammar §1
('dec00003-0001-4001-8001-000006010101', 'Le, zhe, and guo', 'dec00002-0001-4001-8001-000000060101', 'VIDEO', 'https://example.dev/lms/zh/aspect', 780, 0),
('dec00003-0001-4001-8001-000006010102', 'Aspect particle notes', 'dec00002-0001-4001-8001-000000060101', 'ARTICLE', 'https://example.dev/lms/zh/aspect-notes', NULL, 1),
('dec00003-0001-4001-8001-000006010103', 'Aspect quiz', 'dec00002-0001-4001-8001-000000060101', 'QUIZ', NULL, 480, 2),
-- Grammar §2
('dec00003-0001-4001-8001-000006010201', 'Ba construction explained', 'dec00002-0001-4001-8001-000000060102', 'VIDEO', 'https://example.dev/lms/zh/ba', 840, 0),
('dec00003-0001-4001-8001-000006010202', 'Bei construction practice', 'dec00002-0001-4001-8001-000000060102', 'ASSIGNMENT', 'https://example.dev/lms/zh/bei-lab', NULL, 1),
-- Grammar §3
('dec00003-0001-4001-8001-000006010301', 'Result complements', 'dec00002-0001-4001-8001-000000060103', 'ARTICLE', 'https://example.dev/lms/zh/result', 600, 0),
('dec00003-0001-4001-8001-000006010302', 'Direction complements', 'dec00002-0001-4001-8001-000000060103', 'VIDEO', 'https://example.dev/lms/zh/direction-comp', 720, 1),
('dec00003-0001-4001-8001-000006010303', 'Complements quiz', 'dec00002-0001-4001-8001-000000060103', 'QUIZ', NULL, 540, 2),
-- Grammar §4
('dec00003-0001-4001-8001-000006010401', 'Because, although, if', 'dec00002-0001-4001-8001-000000060104', 'ARTICLE', 'https://example.dev/lms/zh/connectors', NULL, 0),
('dec00003-0001-4001-8001-000006010402', 'Write a short paragraph', 'dec00002-0001-4001-8001-000000060104', 'ASSIGNMENT', 'https://example.dev/lms/zh/paragraph-lab', 900, 1),
-- Business §1
('dec00003-0001-4001-8001-000007010101', 'Office greetings', 'dec00002-0001-4001-8001-000000070101', 'VIDEO', 'https://example.dev/lms/zh/biz-greet', 480, 0),
('dec00003-0001-4001-8001-000007010102', 'Name card exchange phrases', 'dec00002-0001-4001-8001-000000070101', 'ARTICLE', 'https://example.dev/lms/zh/biz-card', NULL, 1),
('dec00003-0001-4001-8001-000007010103', 'Greeting quiz', 'dec00002-0001-4001-8001-000000070101', 'QUIZ', NULL, 300, 2),
('dec00003-0001-4001-8001-000007010104', 'Role-play: meeting a client', 'dec00002-0001-4001-8001-000000070101', 'ASSIGNMENT', NULL, NULL, 3),
-- Business §2
('dec00003-0001-4001-8001-000007010201', 'Scheduling a meeting', 'dec00002-0001-4001-8001-000000070102', 'VIDEO', 'https://example.dev/lms/zh/biz-schedule', 660, 0),
('dec00003-0001-4001-8001-000007010202', 'Agenda vocabulary', 'dec00002-0001-4001-8001-000000070102', 'ARTICLE', 'https://example.dev/lms/zh/biz-agenda', 420, 1),
('dec00003-0001-4001-8001-000007010203', 'Meeting language quiz', 'dec00002-0001-4001-8001-000000070102', 'QUIZ', NULL, 360, 2),
-- Business §3
('dec00003-0001-4001-8001-000007010301', 'Email openings and closings', 'dec00002-0001-4001-8001-000000070103', 'ARTICLE', 'https://example.dev/lms/zh/biz-email', NULL, 0),
('dec00003-0001-4001-8001-000007010302', 'Draft a polite request', 'dec00002-0001-4001-8001-000000070103', 'ASSIGNMENT', 'https://example.dev/lms/zh/biz-request', 780, 1),
-- Culture §1
('dec00003-0001-4001-8001-000008010101', 'Spring Festival vocabulary', 'dec00002-0001-4001-8001-000000080101', 'VIDEO', 'https://example.dev/lms/zh/chunjie', 600, 0),
('dec00003-0001-4001-8001-000008010102', 'Mid-Autumn Festival reading', 'dec00002-0001-4001-8001-000000080101', 'ARTICLE', 'https://example.dev/lms/zh/zhongqiu', NULL, 1),
-- Culture §2
('dec00003-0001-4001-8001-000008010201', 'Tea and banquet phrases', 'dec00002-0001-4001-8001-000000080102', 'VIDEO', 'https://example.dev/lms/zh/dining', 540, 0),
('dec00003-0001-4001-8001-000008010202', 'Food culture notes', 'dec00002-0001-4001-8001-000000080102', 'ARTICLE', 'https://example.dev/lms/zh/food-culture', 360, 1),
('dec00003-0001-4001-8001-000008010203', 'Dining etiquette quiz', 'dec00002-0001-4001-8001-000000080102', 'QUIZ', NULL, 300, 2),
-- Culture §3
('dec00003-0001-4001-8001-000008010301', 'Gift-giving language', 'dec00002-0001-4001-8001-000000080103', 'ARTICLE', 'https://example.dev/lms/zh/gifts', NULL, 0),
('dec00003-0001-4001-8001-000008010302', 'Polite refusal practice', 'dec00002-0001-4001-8001-000000080103', 'ASSIGNMENT', 'https://example.dev/lms/zh/refuse-lab', 480, 1),
-- Culture §4
('dec00003-0001-4001-8001-000008010401', 'Short reading: a family dinner', 'dec00002-0001-4001-8001-000000080104', 'ARTICLE', 'https://example.dev/lms/zh/family-dinner', 420, 0),
('dec00003-0001-4001-8001-000008010402', 'Culture review quiz', 'dec00002-0001-4001-8001-000000080104', 'QUIZ', NULL, 360, 1);

COMMIT;
