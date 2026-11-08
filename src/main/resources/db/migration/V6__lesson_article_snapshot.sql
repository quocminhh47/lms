ALTER TABLE lessons
    ADD COLUMN article_extraction_status VARCHAR(16),
    ADD COLUMN article_extracted_title TEXT,
    ADD COLUMN article_author TEXT,
    ADD COLUMN article_published_at TIMESTAMPTZ,
    ADD COLUMN article_featured_image_url TEXT,
    ADD COLUMN article_content_html TEXT,
    ADD COLUMN article_extracted_at TIMESTAMPTZ,
    ADD COLUMN article_extraction_error TEXT;

ALTER TABLE lessons
    ADD CONSTRAINT chk_lessons_article_extraction_status
        CHECK (
            article_extraction_status IS NULL
            OR article_extraction_status IN ('PENDING', 'SUCCESS', 'FAILED')
        );
