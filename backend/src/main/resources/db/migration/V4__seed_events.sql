-- V4: Eventos de prueba para desarrollo
-- Estos eventos coinciden con los MOCK_EVENTS del frontend

INSERT INTO events (id, title, caption, location_text, zone_id, event_date, status)
VALUES
  (
    '11111111-1111-1111-1111-111111111111',
    'Casa Abierta de Medicina y Ciencias de la Salud',
    'Simulaciones médicas, chequeos gratuitos y exposición de proyectos anatómicos.',
    'Plazoleta Central',
    1,
    NOW() + INTERVAL '47 days',
    'APPROVED'
  ),
  (
    '22222222-2222-2222-2222-222222222222',
    'Concierto Sinfónico y Coro UCSG',
    'Nuestra orquesta sinfónica universitaria rinde homenaje a la música ecuatoriana.',
    'Aula Magna',
    1,
    NOW() + INTERVAL '54 days',
    'APPROVED'
  ),
  (
    '33333333-3333-3333-3333-333333333333',
    'Copa Interfacultades de Fútbol',
    'Arranca la temporada deportiva con la gran final entre las facultades.',
    'Cancha de Césped Sintético',
    3,
    NOW() + INTERVAL '60 days',
    'APPROVED'
  ),
  (
    '44444444-4444-4444-4444-444444444444',
    'Congreso de Ingeniería e Inteligencia Artificial',
    'Jornada completa con ponencias sobre IA aplicada, ciberseguridad y desarrollo.',
    'Auditorio Leonidas Ortega',
    6,
    NOW() + INTERVAL '68 days',
    'APPROVED'
  )
ON CONFLICT (id) DO NOTHING;
