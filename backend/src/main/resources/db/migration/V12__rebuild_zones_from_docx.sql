-- ─────────────────────────────────────────────────────────────────────────────
-- V12: Reconstruye por completo la tabla zones con la lista real y verificada
-- que envió el usuario (Location Ucsg.docx — coordenadas tomadas directo de
-- Google Maps para cada edificio del campus UCSG Guayaquil).
--
-- Se borran TODAS las zonas anteriores (aproximaciones/duplicados de rondas
-- previas) — events.zone_id es ON DELETE SET NULL, así que los eventos que
-- ya tenían una zona asignada simplemente quedan sin zona específica (caen
-- al respaldo "Campus Universitario" al mostrarse) en vez de fallar. Es el
-- costo esperado de limpiar datos aproximados por datos reales.
--
-- NOTA: el docx trae "Coliseo UCSG" y "Biblioteca" con coordenadas IDÉNTICAS
-- (-2.182185842, -79.903526675) — es casi seguro un error de copiado en el
-- documento fuente (dos edificios distintos no pueden estar en el mismo
-- punto exacto). Se usa esa coordenada para Biblioteca (viene también de
-- ahí) y se conserva la coordenada de Coliseo ya verificada en una ronda
-- anterior (link individual de Google Maps). Confirmar con el usuario cuál
-- es la correcta para Coliseo si hace falta más precisión.
-- ─────────────────────────────────────────────────────────────────────────────

DELETE FROM zones;

INSERT INTO zones (name, description, match_keywords, location) VALUES

    ('Campus Universitario',
     'Ubicación genérica del campus — respaldo cuando el post no menciona un lugar específico',
     NULL, -- sin keywords a propósito: nunca debe "ganar" el matching automático, solo se usa como opción manual o de respaldo
     ST_SetSRID(ST_MakePoint(-79.904362193, -2.181773084), 4326)),

    ('Facultad de Ingeniería', 'Facultad de Ingeniería',
     'facultad de ingenieria,ingenieria,sistemas,tics',
     ST_SetSRID(ST_MakePoint(-79.904891924, -2.181010544), 4326)),

    ('Auditorio de Ingeniería - Piso 3', 'Auditorio de Ingeniería — Piso 3',
     'auditorio de ingenieria',
     ST_SetSRID(ST_MakePoint(-79.904831577, -2.181178062), 4326)),

    ('Auditorio Leonidas Ortega', 'Auditorio Leonidas Ortega — Piso 2, Edificio Principal',
     'auditorio leonidas ortega,leonidas ortega',
     ST_SetSRID(ST_MakePoint(-79.903131055, -2.181267842), 4326)),

    ('Auditorio Félix Henríquez', 'Auditorio Félix Henríquez',
     'auditorio felix henriquez,felix henriquez',
     ST_SetSRID(ST_MakePoint(-79.905437754, -2.180900653), 4326)),

    ('Aula Magna', 'Aula Magna',
     'aula magna',
     ST_SetSRID(ST_MakePoint(-79.904134202, -2.180770659), 4326)),

    ('Edificio Principal', 'Edificio Principal / Rectorado',
     'edificio principal,rectorado',
     ST_SetSRID(ST_MakePoint(-79.903049239, -2.181191462), 4326)),

    ('Edificio de Posgrado', 'Edificio de Posgrado',
     'posgrado,maestria,edificio de posgrado',
     ST_SetSRID(ST_MakePoint(-79.904785975, -2.180360577), 4326)),

    ('Biblioteca', 'Biblioteca central del campus',
     'biblioteca,biblio',
     ST_SetSRID(ST_MakePoint(-79.903526675, -2.182185842), 4326)),

    ('Coliseo', 'Coliseo — Deportes y Recreación',
     'coliseo',
     ST_SetSRID(ST_MakePoint(-79.904281712, -2.18322577), 4326)),

    ('Cancha de Fútbol UCSG', 'Cancha de fútbol del campus',
     'cancha de futbol,futbol,cancha',
     ST_SetSRID(ST_MakePoint(-79.907020256, -2.18214697), 4326)),

    ('Gimnasio UCSG', 'Gimnasio del campus',
     'gimnasio',
     ST_SetSRID(ST_MakePoint(-79.905016642, -2.181830699), 4326)),

    ('Canchas Medicina UCSG', 'Canchas — Facultad de Medicina',
     'cancha medicina,canchas de medicina',
     ST_SetSRID(ST_MakePoint(-79.905504797, -2.182515512), 4326)),

    ('Facultad de Medicina - Edificio 1', 'Facultad de Ciencias Médicas — Edificio 1',
     'facultad de medicina,ciencias medicas,medicina',
     ST_SetSRID(ST_MakePoint(-79.905807903, -2.182495403), 4326)),

    ('Facultad de Medicina - Edificio 2', 'Facultad de Ciencias Médicas — Edificio 2',
     'medicina edificio 2,facultad de medicina 2',
     ST_SetSRID(ST_MakePoint(-79.905207088, -2.183304843), 4326)),

    ('Clínica Odontológica', 'Clínica Odontológica',
     'clinica odontologica,odontologia',
     ST_SetSRID(ST_MakePoint(-79.905390805, -2.182977848), 4326)),

    ('Facultad de Jurisprudencia', 'Facultad de Jurisprudencia y Ciencias Sociales y Políticas',
     'jurisprudencia,ciencias sociales,ciencias politicas,derecho',
     ST_SetSRID(ST_MakePoint(-79.903338916, -2.181773074), 4326)),

    ('Facultad de Ciencias Económicas', 'Facultad de Ciencias Económicas, Administrativas y Empresariales',
     'ciencias economicas,administrativas,empresariales,economia',
     ST_SetSRID(ST_MakePoint(-79.902540964, -2.182392219), 4326)),

    ('Facultad de Educación Técnica', 'Facultad de Educación Técnica para el Desarrollo',
     'educacion tecnica',
     ST_SetSRID(ST_MakePoint(-79.903069365, -2.183134658), 4326)),

    ('Facultad de Filosofía, Letras y Ciencias de la Educación', 'Facultad de Filosofía, Letras y Ciencias de la Educación',
     'filosofia,letras,ciencias de la educacion',
     ST_SetSRID(ST_MakePoint(-79.90577571, -2.181523812), 4326)),

    ('Facultad de Arquitectura y Diseño', 'Facultad de Arquitectura y Diseño',
     'arquitectura,diseño',
     ST_SetSRID(ST_MakePoint(-79.905357286, -2.180995801), 4326)),

    ('Facultad de Artes y Humanidades', 'Facultad de Artes y Humanidades',
     'artes,humanidades',
     ST_SetSRID(ST_MakePoint(-79.903324174, -2.181271868), 4326)),

    ('Centro de Idiomas', 'Centro / Academia de Idiomas',
     'centro de idiomas,idiomas,academia de idiomas',
     ST_SetSRID(ST_MakePoint(-79.903603124, -2.181518453), 4326)),

    ('Televisión y Radio UCSG', 'Medios UCSG — Televisión y Radio',
     'television,radio,medios ucsg',
     ST_SetSRID(ST_MakePoint(-79.902747495, -2.183315569), 4326)),

    ('Banco Pichincha', 'Agencia Banco Pichincha — campus UCSG',
     'banco pichincha,banco',
     ST_SetSRID(ST_MakePoint(-79.903721139, -2.182429744), 4326)),

    ('Restaurantes', 'Área de restaurantes/comida del campus',
     'restaurantes,comida,cafeteria',
     ST_SetSRID(ST_MakePoint(-79.904407782, -2.182598602), 4326)),

    ('Capilla', 'Capilla universitaria',
     'capilla',
     ST_SetSRID(ST_MakePoint(-79.904386324, -2.182105433), 4326)),

    ('Federación de Estudiantes', 'Sede de la Federación de Estudiantes UCSG',
     'federacion de estudiantes,feuc',
     ST_SetSRID(ST_MakePoint(-79.904076529, -2.182317174), 4326)),

    ('Estacionamiento', 'Estacionamiento del campus',
     'estacionamiento,parqueadero',
     ST_SetSRID(ST_MakePoint(-79.905001883, -2.182282333), 4326)),

    ('Plaza Qatar', 'Plaza Qatar — campus UCSG',
     'plaza qatar,plaza',
     ST_SetSRID(ST_MakePoint(-79.904344743, -2.182483351), 4326));
