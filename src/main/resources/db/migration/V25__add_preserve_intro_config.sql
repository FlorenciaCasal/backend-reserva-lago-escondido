insert into system_config (config_key, config_value)
values (
  'preservar_intro',
  'Conservamos la biodiversidad y los ecosistemas para las generaciones presentes y futuras.'
)
on conflict (config_key) do nothing;
