ALTER TABLE project_documents ADD COLUMN IF NOT EXISTS media_asset_id UUID;
ALTER TABLE project_advances ADD COLUMN IF NOT EXISTS image_asset_id UUID;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_name = 'fk_project_documents_media_asset'
      AND table_name = 'project_documents'
  ) THEN
    ALTER TABLE project_documents
      ADD CONSTRAINT fk_project_documents_media_asset
      FOREIGN KEY (media_asset_id) REFERENCES media_assets(id) ON DELETE SET NULL;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_name = 'fk_project_advances_image_asset'
      AND table_name = 'project_advances'
  ) THEN
    ALTER TABLE project_advances
      ADD CONSTRAINT fk_project_advances_image_asset
      FOREIGN KEY (image_asset_id) REFERENCES media_assets(id) ON DELETE SET NULL;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_project_documents_media_asset_id ON project_documents(media_asset_id);
CREATE INDEX IF NOT EXISTS idx_project_advances_image_asset_id ON project_advances(image_asset_id);