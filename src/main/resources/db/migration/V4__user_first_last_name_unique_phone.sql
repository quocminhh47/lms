-- Split display name into first/last name and make phone number a unique login identifier.
--
-- full_name is KEPT (NOT NULL, unchanged) and remains the display name used by the API.
-- New registrations write first_name, last_name, and full_name = first_name || ' ' || last_name.
--
-- Backfill strategy for existing accounts (documented, best-effort):
--   first_name = first whitespace-delimited token of full_name
--   last_name  = the remainder (NULL when full_name is a single token)
-- Name order differs between cultures, so this split is only a starting point; no name is
-- discarded because full_name is preserved verbatim. Legacy rows may keep NULL first/last
-- names until the user edits their profile.

ALTER TABLE users
    ADD COLUMN first_name VARCHAR(100),
    ADD COLUMN last_name VARCHAR(100);

UPDATE users
SET first_name = NULLIF(left(split_part(btrim(full_name), ' ', 1), 100), ''),
    last_name  = NULLIF(
        left(btrim(substr(btrim(full_name), length(split_part(btrim(full_name), ' ', 1)) + 1)), 100),
        '')
WHERE first_name IS NULL;

-- Canonical phone form: optional leading '+' followed by digits only (matches application code).
UPDATE users
SET phone = NULLIF(
        CASE WHEN btrim(phone) LIKE '+%'
             THEN '+' || regexp_replace(phone, '[^0-9]', '', 'g')
             ELSE regexp_replace(phone, '[^0-9]', '', 'g')
        END,
        '')
WHERE phone IS NOT NULL;

UPDATE users SET phone = NULL WHERE phone = '+';

-- Fails loudly (rather than discarding data) if two existing users share a phone number.
-- PostgreSQL allows many NULL phones under a UNIQUE constraint.
ALTER TABLE users
    ADD CONSTRAINT uq_users_phone UNIQUE (phone);
