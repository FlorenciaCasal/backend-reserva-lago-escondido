create table if not exists project_documents (
  id uuid primary key,
  project_id uuid not null references projects(id) on delete cascade,
  title varchar(180) not null,
  description varchar(600),
  file_url varchar(500) not null,
  file_type varchar(80),
  sort_order integer not null default 0,
  created_at timestamp not null default now(),
  updated_at timestamp not null default now()
);

create index if not exists idx_project_documents_project_order
  on project_documents (project_id, sort_order asc, created_at asc);
