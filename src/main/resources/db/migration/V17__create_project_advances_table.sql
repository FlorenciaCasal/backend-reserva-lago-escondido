create table if not exists project_advances (
  id uuid primary key,
  project_id uuid not null references projects(id) on delete cascade,
  advance_date date not null,
  title varchar(180) not null,
  description text not null,
  image_url varchar(500),
  video_url varchar(500),
  created_at timestamp not null default now(),
  updated_at timestamp not null default now()
);

create index if not exists idx_project_advances_project_date
  on project_advances (project_id, advance_date desc, created_at desc);
