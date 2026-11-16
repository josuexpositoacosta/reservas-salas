-- Sólo H2. Bug de H2 2.4: un CHECK con IN (...) —o con OR, que H2 convierte en IN— guarda una
-- referencia a la sesión que lo creó (la de Flyway). Cuando esa sesión se cierra, cualquier INSERT
-- falla con "Check constraint invalid ... The database has been closed" y la API responde 409.
-- Con "= ANY(ARRAY[...])" H2 no usa ese camino y la restricción sigue validando igual.
ALTER TABLE reservas DROP CONSTRAINT ck_reservas_estado;

ALTER TABLE reservas
    ADD CONSTRAINT ck_reservas_estado CHECK (estado = ANY (ARRAY['ACTIVA', 'CANCELADA']));
