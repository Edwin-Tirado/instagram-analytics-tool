-- ─────────────────────────────────────────────────────────────────────────────
-- V11: Coordenadas reales, verificadas por el usuario directo desde Google
-- Maps (clic en cada edificio del campus UCSG Guayaquil → link de dirección),
-- comparadas y cruzadas con el mapa de referencia scribblemaps D8IRPbqRGi.
--
-- Actualiza 5 zonas existentes cuya posición aproximada quedaba a 50-150m
-- del edificio real, y agrega 11 zonas reales que aparecían en el mapa de
-- referencia pero no existían todavía en esta tabla.
-- ─────────────────────────────────────────────────────────────────────────────

-- ── Correcciones a zonas existentes ─────────────────────────────────────────

UPDATE zones SET location = ST_SetSRID(ST_MakePoint(-79.905744857, -2.182557043), 4326)
    WHERE name = 'Facultades de Medicina';

UPDATE zones SET location = ST_SetSRID(ST_MakePoint(-79.90472964,  -2.180431595), 4326)
    WHERE name = 'Edificio de Posgrados';

UPDATE zones SET location = ST_SetSRID(ST_MakePoint(-79.9035173,   -2.1822609), 4326)
    WHERE name = 'Biblioteca';

UPDATE zones SET location = ST_SetSRID(ST_MakePoint(-79.9042965,   -2.1826106), 4326)
    WHERE name = 'Cafetería';

UPDATE zones SET location = ST_SetSRID(ST_MakePoint(-79.904356811, -2.182081296), 4326)
    WHERE name = 'Capilla Universitaria';

-- ── Zonas reales nuevas (no existían) ───────────────────────────────────────

INSERT INTO zones (name, description, match_keywords, location) VALUES
    ('Facultad de Ciencias Económicas',
     'Facultad de Ciencias Económicas, Administrativas y Empresariales',
     'ciencias económicas,administrativas,empresariales,economía',
     ST_SetSRID(ST_MakePoint(-79.902597276, -2.182364072), 4326)),

    ('Facultad de Educación Técnica',
     'Facultad de Educación Técnica para el Desarrollo',
     'educación técnica,educacion tecnica',
     ST_SetSRID(ST_MakePoint(-79.9028735, -2.1832244), 4326)),

    ('Banco Pichincha',
     'Agencia Banco Pichincha — campus UCSG',
     'banco pichincha,banco',
     ST_SetSRID(ST_MakePoint(-79.9037399, -2.1824257), 4326)),

    ('Facultad de Filosofía, Letras y Ciencias de la Educación',
     'Facultad de Filosofía, Letras y Ciencias de la Educación',
     'filosofía,letras,ciencias de la educación',
     ST_SetSRID(ST_MakePoint(-79.90577034, -2.18154658), 4326)),

    ('Federación de Estudiantes',
     'Sede de la Federación de Estudiantes UCSG',
     'federación de estudiantes,feuc',
     ST_SetSRID(ST_MakePoint(-79.9040645, -2.1823064), 4326)),

    ('Facultad de Jurisprudencia',
     'Facultad de Jurisprudencia y Ciencias Sociales y Políticas',
     'jurisprudencia,ciencias sociales,ciencias políticas,derecho',
     ST_SetSRID(ST_MakePoint(-79.903452909, -2.181703375), 4326)),

    ('Coliseo',
     'Coliseo — Deportes y Recreación',
     'coliseo,deportes',
     ST_SetSRID(ST_MakePoint(-79.904281712, -2.18322577), 4326)),

    ('Edificio Principal',
     'Edificio Principal / Rectorado del campus UCSG',
     'edificio principal,rectorado',
     ST_SetSRID(ST_MakePoint(-79.903206153, -2.181191456), 4326)),

    ('Estacionamiento',
     'Estacionamiento del campus UCSG',
     'estacionamiento,parqueadero',
     ST_SetSRID(ST_MakePoint(-79.904988476, -2.182313142), 4326)),

    ('Plaza Qatar',
     'Plaza Qatar — campus UCSG',
     'plaza qatar,plaza',
     ST_SetSRID(ST_MakePoint(-79.9043474, -2.1824847), 4326)),

    ('Facultad de Artes y Humanidades',
     'Facultad de Artes y Humanidades',
     'artes,humanidades',
     ST_SetSRID(ST_MakePoint(-79.903407304, -2.181326794), 4326))
ON CONFLICT DO NOTHING;
