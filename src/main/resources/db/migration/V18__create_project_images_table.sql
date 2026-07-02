create table if not exists project_images (
  id uuid primary key,
  project_id uuid not null references projects(id) on delete cascade,
  image_url varchar(500) not null,
  alt_text varchar(180),
  caption varchar(300),
  sort_order integer not null default 0,
  created_at timestamp not null default now(),
  updated_at timestamp not null default now()
);

create index if not exists idx_project_images_project_order
  on project_images (project_id, sort_order asc, created_at asc);
