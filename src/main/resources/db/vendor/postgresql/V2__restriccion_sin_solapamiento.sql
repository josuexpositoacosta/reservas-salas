-- Sólo PostgreSQL (H2 no soporta restricciones de exclusión).
-- Garantiza en la propia base de datos que no hay dos reservas ACTIVAS solapadas en la misma sala,
-- aunque alguien escriba sin pasar por la API. '[)' = intervalo semiabierto, igual que en el código.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservas
    ADD CONSTRAINT ex_reservas_sin_solapamiento
    EXCLUDE USING gist (sala_id WITH =, tsrange(inicio, fin, '[)') WITH &&)
    WHERE (estado = 'ACTIVA');
