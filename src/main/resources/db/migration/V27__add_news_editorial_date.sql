ALTER TABLE news
    ADD COLUMN IF NOT EXISTS editorial_date date;

UPDATE news
SET editorial_date = COALESCE(published_at::date, created_at::date)
WHERE editorial_date IS NULL;

ALTER TABLE news
    ALTER COLUMN editorial_date SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_news_status_editorial_date
    ON news(status, editorial_date DESC, published_at DESC, created_at DESC);
