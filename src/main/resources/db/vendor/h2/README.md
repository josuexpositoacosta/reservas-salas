Migraciones específicas de H2. Ver `db/vendor/postgresql` para las específicas de PostgreSQL.

- `V2__ck_estado_compatible_h2.sql`: rehace `ck_reservas_estado` sin `IN (...)` para esquivar un bug
  de H2 2.4 (el CHECK dejaba de funcionar al cerrarse la sesión de Flyway y todo INSERT daba 409).
