-- Standalone CMS articles (not tied to courses or lessons).
-- Public visibility is published_at IS NOT NULL AND archived_at IS NULL;
-- working status may differ while a published snapshot remains live.

CREATE TABLE articles (
    id                          UUID            PRIMARY KEY,
    author_id                   UUID            NOT NULL,
    slug                        VARCHAR(200)    NOT NULL,
    status                      VARCHAR         NOT NULL DEFAULT 'DRAFT',
    title                       VARCHAR         NOT NULL,
    excerpt                     TEXT,
    featured_image_url          VARCHAR(2048),
    content_html                TEXT            NOT NULL DEFAULT '',
    published_title             VARCHAR,
    published_excerpt           TEXT,
    published_featured_image_url VARCHAR(2048),
    published_content_html      TEXT,
    rejection_reason            TEXT,
    submitted_at                TIMESTAMPTZ,
    reviewed_by_id              UUID,
    reviewed_at                 TIMESTAMPTZ,
    published_at                TIMESTAMPTZ,
    archived_at                 TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_articles_author FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT fk_articles_reviewed_by FOREIGN KEY (reviewed_by_id) REFERENCES users (id),
    CONSTRAINT uq_articles_slug UNIQUE (slug),
    CONSTRAINT chk_articles_status CHECK (
        status IN ('DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'REJECTED', 'ARCHIVED')
    )
);

CREATE INDEX idx_articles_author_id ON articles (author_id);

CREATE INDEX idx_articles_pending_review_submitted_at
    ON articles (submitted_at)
    WHERE status = 'PENDING_REVIEW';

CREATE INDEX idx_articles_public_published_at
    ON articles (published_at DESC)
    WHERE published_at IS NOT NULL AND archived_at IS NULL;
