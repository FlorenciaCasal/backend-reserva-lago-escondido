alter table projects
  add column if not exists video_url varchar(500),
  add column if not exists featured boolean not null default false,
  add column if not exists published_at timestamp,
  add column if not exists archived_at timestamp;

alter table projects drop constraint if exists projects_status_check;

alter table projects
  add constraint projects_status_check
  check (status in ('DRAFT', 'PUBLISHED', 'ARCHIVED'));

update projects
set published_at = created_at
where status = 'PUBLISHED' and published_at is null;
