-- Course catalog thumbnail and optional user profile fields for UI redesign.

ALTER TABLE courses
    ADD COLUMN thumbnail_url VARCHAR(2048);

ALTER TABLE users
    ADD COLUMN phone VARCHAR(32),
    ADD COLUMN date_of_birth DATE;
