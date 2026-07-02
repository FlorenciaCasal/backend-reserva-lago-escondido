create table if not exists projects (
  id uuid primary key,
  title varchar(140) not null,
  summary varchar(600) not null,
  content text not null,
  slug varchar(160) not null unique,
  image_url varchar(500),
  status varchar(20) not null check (status in ('DRAFT', 'PUBLISHED')),
  created_at timestamp not null default now(),
  updated_at timestamp not null default now()
);

insert into projects (id, title, summary, content, slug, image_url, status)
select
  '11111111-1111-4111-8111-111111111111',
  'HUEMUL',
  'Proyecto orientado a la conservacion del huemul y su habitat natural.',
  'El proyecto HUEMUL acompana las acciones de conservacion de una especie emblematica de los ambientes cordilleranos. Promueve el cuidado del habitat, la observacion responsable y la educacion ambiental como herramientas para proteger la biodiversidad de la reserva.',
  'huemul',
  '/img/huemul2.png',
  'PUBLISHED'
where not exists (select 1 from projects where slug = 'huemul');

insert into projects (id, title, summary, content, slug, image_url, status)
select
  '22222222-2222-4222-8222-222222222222',
  'ALERCE',
  'Iniciativa de proteccion y valoracion de bosques nativos y especies longevas.',
  'El proyecto ALERCE busca fortalecer la valoracion de los bosques nativos, su historia natural y su rol en el equilibrio del ambiente. Desde la reserva se promueve el conocimiento del entorno y la visita responsable para conservar estos paisajes para las generaciones futuras.',
  'alerce',
  '/img/alerces.jpg',
  'PUBLISHED'
where not exists (select 1 from projects where slug = 'alerce');

insert into projects (id, title, summary, content, slug, image_url, status)
select
  '33333333-3333-4333-8333-333333333333',
  'DIDYMO',
  'Acciones de prevencion y concientizacion sobre especies invasoras acuaticas.',
  'El proyecto DIDYMO impulsa la prevencion, informacion y buenas practicas para evitar la dispersion de especies invasoras en ambientes acuaticos. La participacion responsable de visitantes y comunidad es clave para proteger los cursos de agua y su biodiversidad.',
  'didymo',
  '/img/didymo.jpg',
  'PUBLISHED'
where not exists (select 1 from projects where slug = 'didymo');
