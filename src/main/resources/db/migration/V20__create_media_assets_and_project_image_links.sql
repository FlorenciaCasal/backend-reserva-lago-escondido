CREATE TABLE IF NOT EXISTS media_assets (
    id UUID PRIMARY KEY,
    kind VARCHAR(30) NOT NULL,
    storage_provider VARCHAR(30) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE projects ADD COLUMN IF NOT EXISTS image_asset_id UUID;
ALTER TABLE project_images ADD COLUMN IF NOT EXISTS media_asset_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_projects_image_asset'
    ) THEN
        ALTER TABLE projects
            ADD CONSTRAINT fk_projects_image_asset
            FOREIGN KEY (image_asset_id) REFERENCES media_assets(id)
            ON DELETE SET NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_project_images_media_asset'
    ) THEN
        ALTER TABLE project_images
            ADD CONSTRAINT fk_project_images_media_asset
            FOREIGN KEY (media_asset_id) REFERENCES media_assets(id)
            ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_projects_image_asset_id ON projects(image_asset_id);
CREATE INDEX IF NOT EXISTS idx_project_images_media_asset_id ON project_images(media_asset_id);
CREATE INDEX IF NOT EXISTS idx_media_assets_kind ON media_assets(kind);