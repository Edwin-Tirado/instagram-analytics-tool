-- ─────────────────────────────────────────────────────────────────────────────
-- V10: Corrige las coordenadas de las 6 zonas originales de V1__init_schema.sql
--
-- Estaban centradas ~3km al norte-este de la ubicación real del campus UCSG
-- Guayaquil (verificada: -2.1757, -79.9023 — Av. Carlos Julio Arosemena Km 1.5),
-- mientras que las 16 zonas agregadas en V6 (basadas en FACILITY_COORDINATES
-- del frontend) sí caían cerca de esa ubicación real. Se aplica el mismo
-- desplazamiento fijo a las 6 originales, preservando su posición relativa
-- entre sí, para que caigan dentro del mismo campus real que las demás.
-- ─────────────────────────────────────────────────────────────────────────────

UPDATE zones SET location = ST_SetSRID(ST_MakePoint(-79.904533, -2.181610), 4326) WHERE name = 'Auditorio Principal';
UPDATE zones SET location = ST_SetSRID(ST_MakePoint(-79.904933, -2.181910), 4326) WHERE name = 'Biblioteca';
UPDATE zones SET location = ST_SetSRID(ST_MakePoint(-79.903933, -2.182610), 4326) WHERE name = 'Cancha Deportiva';
UPDATE zones SET location = ST_SetSRID(ST_MakePoint(-79.904733, -2.181410), 4326) WHERE name = 'Cafetería';
UPDATE zones SET location = ST_SetSRID(ST_MakePoint(-79.905133, -2.181210), 4326) WHERE name = 'Bloque Administrativo';
UPDATE zones SET location = ST_SetSRID(ST_MakePoint(-79.904333, -2.182210), 4326) WHERE name = 'Facultad de Ingeniería';
