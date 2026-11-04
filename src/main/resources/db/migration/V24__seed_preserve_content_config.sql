alter table system_config
    alter column config_value type text;

insert into system_config (config_key, config_value)
values
  (
    'preservar_what_we_do_text',
    'Trabajamos en la proteccion de especies nativas, la restauracion de ambientes y la investigacion cientifica para comprender y cuidar nuestro entorno. Nuestro compromiso es integral y se basa en pilares tecnicos y educativos.'
  ),
  (
    'preservar_what_we_do_bullets',
    '["Conservacion de especies nativas","Restauracion de ecosistemas","Investigacion y monitoreo","Educacion ambiental"]'
  )
on conflict (config_key) do nothing;
