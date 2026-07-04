-- ─────────────────────────────────────────────────────────────────────────────
-- V8: Imágenes para los eventos de prueba de V4 (no tenían ninguna)
-- Placeholders estables (Lorem Picsum, semilla fija) — solo para fines
-- didácticos/desarrollo, no son fotos reales de los eventos.
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO event_images (event_id, media_url, display_order) VALUES
    ('11111111-1111-1111-1111-111111111111', 'https://picsum.photos/seed/casa-abierta-medicina/800/600', 0),
    ('22222222-2222-2222-2222-222222222222', 'https://picsum.photos/seed/concierto-sinfonico-ucsg/800/600', 0),
    ('33333333-3333-3333-3333-333333333333', 'https://picsum.photos/seed/copa-interfacultades-futbol/800/600', 0),
    ('44444444-4444-4444-4444-444444444444', 'https://picsum.photos/seed/congreso-ingenieria-ia/800/600', 0)
ON CONFLICT DO NOTHING;
