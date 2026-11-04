CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS project_gallery_items (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    kind VARCHAR(20) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    media_asset_id UUID REFERENCES media_assets(id) ON DELETE SET NULL,
    source_url VARCHAR(500),
    external_provider VARCHAR(30),
    external_video_id VARCHAR(100),
    caption VARCHAR(300),
    alt_text VARCHAR(180),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_project_gallery_kind CHECK (kind IN ('IMAGE', 'VIDEO')),
    CONSTRAINT ck_project_gallery_source_type CHECK (source_type IN ('MEDIA_ASSET', 'EXTERNAL_YOUTUBE')),
    CONSTRAINT ck_project_gallery_provider CHECK (external_provider IS NULL OR external_provider IN ('YOUTUBE'))
);

CREATE TABLE IF NOT EXISTS project_advance_gallery_items (
    id UUID PRIMARY KEY,
    advance_id UUID NOT NULL REFERENCES project_advances(id) ON DELETE CASCADE,
    kind VARCHAR(20) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    media_asset_id UUID REFERENCES media_assets(id) ON DELETE SET NULL,
    source_url VARCHAR(500),
    external_provider VARCHAR(30),
    external_video_id VARCHAR(100),
    caption VARCHAR(300),
    alt_text VARCHAR(180),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_project_advance_gallery_kind CHECK (kind IN ('IMAGE', 'VIDEO')),
    CONSTRAINT ck_project_advance_gallery_source_type CHECK (source_type IN ('MEDIA_ASSET', 'EXTERNAL_YOUTUBE')),
    CONSTRAINT ck_project_advance_gallery_provider CHECK (external_provider IS NULL OR external_provider IN ('YOUTUBE'))
);

CREATE TABLE IF NOT EXISTS news_gallery_items (
    id UUID PRIMARY KEY,
    news_id UUID NOT NULL REFERENCES news(id) ON DELETE CASCADE,
    kind VARCHAR(20) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    media_asset_id UUID REFERENCES media_assets(id) ON DELETE SET NULL,
    source_url VARCHAR(500),
    external_provider VARCHAR(30),
    external_video_id VARCHAR(100),
    caption VARCHAR(300),
    alt_text VARCHAR(180),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_news_gallery_kind CHECK (kind IN ('IMAGE', 'VIDEO')),
    CONSTRAINT ck_news_gallery_source_type CHECK (source_type IN ('MEDIA_ASSET', 'EXTERNAL_YOUTUBE')),
    CONSTRAINT ck_news_gallery_provider CHECK (external_provider IS NULL OR external_provider IN ('YOUTUBE'))
);

CREATE INDEX IF NOT EXISTS idx_project_gallery_items_project_order ON project_gallery_items(project_id, sort_order, created_at);
CREATE INDEX IF NOT EXISTS idx_project_gallery_items_media_asset ON project_gallery_items(media_asset_id);
CREATE INDEX IF NOT EXISTS idx_project_advance_gallery_items_advance_order ON project_advance_gallery_items(advance_id, sort_order, created_at);
CREATE INDEX IF NOT EXISTS idx_project_advance_gallery_items_media_asset ON project_advance_gallery_items(media_asset_id);
CREATE INDEX IF NOT EXISTS idx_news_gallery_items_news_order ON news_gallery_items(news_id, sort_order, created_at);
CREATE INDEX IF NOT EXISTS idx_news_gallery_items_media_asset ON news_gallery_items(media_asset_id);

INSERT INTO project_gallery_items (id, project_id, kind, source_type, media_asset_id, source_url, caption, alt_text, sort_order, created_at, updated_at)
SELECT id, project_id, 'IMAGE', 'MEDIA_ASSET', media_asset_id, image_url, caption, alt_text, sort_order, created_at, updated_at
FROM project_images
WHERE NOT EXISTS (SELECT 1 FROM project_gallery_items pgi WHERE pgi.id = project_images.id);

INSERT INTO news_gallery_items (id, news_id, kind, source_type, media_asset_id, source_url, caption, alt_text, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), id, 'VIDEO', 'MEDIA_ASSET', video_asset_id, video_url, NULL, NULL, 0, created_at, updated_at
FROM news
WHERE (video_asset_id IS NOT NULL OR NULLIF(TRIM(video_url), '') IS NOT NULL)
  AND NOT EXISTS (
      SELECT 1 FROM news_gallery_items ngi
      WHERE ngi.news_id = news.id AND ngi.kind = 'VIDEO' AND COALESCE(ngi.source_url, '') = COALESCE(news.video_url, '')
  );

INSERT INTO news_gallery_items (id, news_id, kind, source_type, media_asset_id, source_url, caption, alt_text, sort_order, created_at, updated_at)
SELECT id, news_id, 'IMAGE', 'MEDIA_ASSET', media_asset_id, image_url, caption, alt_text, sort_order + 1, created_at, updated_at
FROM news_images
WHERE NOT EXISTS (SELECT 1 FROM news_gallery_items ngi WHERE ngi.id = news_images.id);

INSERT INTO project_advance_gallery_items (id, advance_id, kind, source_type, media_asset_id, source_url, caption, alt_text, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), id, 'IMAGE', 'MEDIA_ASSET', image_asset_id, image_url, NULL, title, 0, created_at, updated_at
FROM project_advances
WHERE (image_asset_id IS NOT NULL OR NULLIF(TRIM(image_url), '') IS NOT NULL)
  AND NOT EXISTS (
      SELECT 1 FROM project_advance_gallery_items pagi
      WHERE pagi.advance_id = project_advances.id AND pagi.kind = 'IMAGE' AND COALESCE(pagi.source_url, '') = COALESCE(project_advances.image_url, '')
  );

INSERT INTO project_advance_gallery_items (id, advance_id, kind, source_type, media_asset_id, source_url, caption, alt_text, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), id, 'VIDEO', 'MEDIA_ASSET', video_asset_id, video_url, NULL, NULL, 1, created_at, updated_at
FROM project_advances
WHERE (video_asset_id IS NOT NULL OR NULLIF(TRIM(video_url), '') IS NOT NULL)
  AND NOT EXISTS (
      SELECT 1 FROM project_advance_gallery_items pagi
      WHERE pagi.advance_id = project_advances.id AND pagi.kind = 'VIDEO' AND COALESCE(pagi.source_url, '') = COALESCE(project_advances.video_url, '')
  );
