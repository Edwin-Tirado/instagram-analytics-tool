-- ─────────────────────────────────────────────────────────────────────────────
-- V6: Zonas adicionales — mismos espacios listados en el footer público
-- (frontend/src/lib/mockData.ts → FOOTER_COLS / FACILITY_COORDINATES)
-- Permite que el selector "Editar ubicación" de eventos incluya todas las
-- instalaciones que ya se muestran en el footer de la cartelera pública.
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO zones (name, description, match_keywords, location) VALUES
    -- Campus Principal
    ('Facultades de Ingeniería',   'Facultades de Ingeniería — Campus Principal',      'facultad de ingeniería,facultades de ingeniería,ingeniería',   ST_SetSRID(ST_MakePoint(-79.904848, -2.181166), 4326)),
    ('Facultades de Medicina',     'Facultades de Medicina — Campus Principal',        'facultad de medicina,facultades de medicina,ciencias médicas', ST_SetSRID(ST_MakePoint(-79.904533, -2.179836), 4326)),
    ('Edificio de Posgrados',      'Edificio de Posgrados — Campus Principal',         'posgrado,posgrados,maestría',                                  ST_SetSRID(ST_MakePoint(-79.903901, -2.180424), 4326)),
    ('Clínica Odontológica',       'Clínica Odontológica — Campus Principal',          'clínica odontológica,odontología,odontologica',                ST_SetSRID(ST_MakePoint(-79.904153, -2.180018), 4326)),

    -- Auditorios
    ('Aula Magna',                 'Aula Magna — Auditorios',                          'aula magna',                                                   ST_SetSRID(ST_MakePoint(-79.904107, -2.180823), 4326)),
    ('Auditorio Leonidas Ortega',  'Auditorio Leonidas Ortega — Auditorios',           'auditorio leonidas ortega,leonidas ortega',                    ST_SetSRID(ST_MakePoint(-79.904630, -2.181285), 4326)),
    ('Salón de Usos Múltiples',    'Salón de Usos Múltiples — Auditorios',             'salón de usos múltiples,sum',                                  ST_SetSRID(ST_MakePoint(-79.904420, -2.182190), 4326)),
    ('Salas de Conferencias IT',   'Salas de Conferencias IT — Auditorios',            'sala de conferencias,conferencias it',                         ST_SetSRID(ST_MakePoint(-79.904910, -2.181340), 4326)),

    -- Deportes y Recreación
    ('Canchas Múltiples',          'Canchas Múltiples — Deportes y Recreación',        'cancha múltiple,canchas múltiples',                            ST_SetSRID(ST_MakePoint(-79.905100, -2.183200), 4326)),
    ('Cancha de Césped Sintético', 'Cancha de Césped Sintético — Deportes y Recreación','césped sintético,cancha de fútbol',                            ST_SetSRID(ST_MakePoint(-79.905500, -2.183800), 4326)),
    ('Piscina Universitaria',      'Piscina Universitaria — Deportes y Recreación',    'piscina',                                                       ST_SetSRID(ST_MakePoint(-79.905300, -2.183500), 4326)),
    ('Gimnasio UCSG',              'Gimnasio UCSG — Deportes y Recreación',            'gimnasio',                                                      ST_SetSRID(ST_MakePoint(-79.905000, -2.183000), 4326)),

    -- Servicios Estudiantiles
    ('Biblioteca General',         'Biblioteca General — Servicios Estudiantiles',     'biblioteca general',                                           ST_SetSRID(ST_MakePoint(-79.904322, -2.181543), 4326)),
    ('Medios UCSG (Radio/TV)',     'Medios UCSG (Radio/TV) — Servicios Estudiantiles', 'radio,tv,medios ucsg',                                         ST_SetSRID(ST_MakePoint(-79.904600, -2.182800), 4326)),
    ('Capilla Universitaria',      'Capilla Universitaria — Servicios Estudiantiles',  'capilla',                                                       ST_SetSRID(ST_MakePoint(-79.904000, -2.182350), 4326)),
    ('CoWorking Space',            'CoWorking Space — Servicios Estudiantiles',        'coworking,co-working',                                         ST_SetSRID(ST_MakePoint(-79.904250, -2.181950), 4326))
ON CONFLICT DO NOTHING;
