ALTER TABLE projects ADD COLUMN IF NOT EXISTS video_asset_id UUID;
ALTER TABLE project_advances ADD COLUMN IF NOT EXISTS video_asset_id UUID;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_name = 'fk_projects_video_asset'
      AND table_name = 'projects'
  ) THEN
    ALTER TABLE projects
      ADD CONSTRAINT fk_projects_video_asset
      FOREIGN KEY (video_asset_id) REFERENCES media_assets(id) ON DELETE SET NULL;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_name = 'fk_project_advances_video_asset'
      AND table_name = 'project_advances'
  ) THEN
    ALTER TABLE project_advances
      ADD CONSTRAINT fk_project_advances_video_asset
      FOREIGN KEY (video_asset_id) REFERENCES media_assets(id) ON DELETE SET NULL;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_projects_video_asset_id ON projects(video_asset_id);
CREATE INDEX IF NOT EXISTS idx_project_advances_video_asset_id ON project_advances(video_asset_id);