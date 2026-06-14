-- ─────────────────────────────────────────────────────────────────────────────
-- V1: Esquema inicial — UCSG Campus Analytics
-- ─────────────────────────────────────────────────────────────────────────────

-- Habilitar PostGIS (requiere superusuario en el primer despliegue)
CREATE EXTENSION IF NOT EXISTS postgis;

-- ── Roles ─────────────────────────────────────────────────────────────────────
CREATE TABLE roles (
    id   BIGSERIAL    PRIMARY KEY,
    name VARCHAR(30)  NOT NULL UNIQUE
);

-- Seed de roles (idempotente)
INSERT INTO roles (name) VALUES
    ('ROLE_ADMIN'),
    ('ROLE_SUPERVISOR'),
    ('ROLE_USER')
ON CONFLICT (name) DO NOTHING;

-- ── Usuarios ──────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email                   VARCHAR(150) NOT NULL UNIQUE,
    password                VARCHAR(255) NOT NULL,
    full_name               VARCHAR(120),
    failed_login_attempts   INT          NOT NULL DEFAULT 0,
    locked                  BOOLEAN      NOT NULL DEFAULT FALSE,
    lock_time               TIMESTAMP,
    enabled                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

-- ── Relación usuario-rol ───────────────────────────────────────────────────────
CREATE TABLE user_roles (
    user_id UUID   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ── Zonas del campus ──────────────────────────────────────────────────────────
CREATE TABLE zones (
    id             BIGSERIAL    PRIMARY KEY,
    name           VARCHAR(120) NOT NULL,
    description    VARCHAR(255),
    location       GEOMETRY(Point, 4326),
    match_keywords VARCHAR(500)
);

CREATE INDEX idx_zones_name     ON zones(name);
CREATE INDEX idx_zones_location ON zones USING GIST(location);

-- Seed de zonas (basado en el mapa de referencia scribblemaps D8IRPbqRGi)
-- Las coordenadas son aproximaciones del campus UCSG — ajustar tras
-- revisar el mapa oficial con el equipo.
INSERT INTO zones (name, description, match_keywords, location) VALUES
    ('Auditorio Principal',  'Auditorio central del campus',            'auditorio,auditorio principal',         ST_SetSRID(ST_MakePoint(-79.8946, -2.1500), 4326)),
    ('Biblioteca',           'Edificio de biblioteca universitaria',    'biblioteca,biblio',                     ST_SetSRID(ST_MakePoint(-79.8950, -2.1503), 4326)),
    ('Cancha Deportiva',     'Complejo deportivo principal',            'cancha,deportivo,estadio,deportes',     ST_SetSRID(ST_MakePoint(-79.8940, -2.1510), 4326)),
    ('Cafetería',            'Área de alimentación estudiantil',        'cafetería,cafeteria,comedor',           ST_SetSRID(ST_MakePoint(-79.8948, -2.1498), 4326)),
    ('Bloque Administrativo','Edificio de administración y rectorado',  'administración,administracion,rectorado',ST_SetSRID(ST_MakePoint(-79.8952, -2.1496), 4326)),
    ('Facultad de Ingeniería','Facultad de Sistemas e Ingeniería',     'ingeniería,ingenieria,sistemas,tics',   ST_SetSRID(ST_MakePoint(-79.8944, -2.1506), 4326))
ON CONFLICT DO NOTHING;

-- ── Eventos ───────────────────────────────────────────────────────────────────
CREATE TABLE events (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title               VARCHAR(255) NOT NULL,
    caption             TEXT,
    location_text       VARCHAR(255),
    zone_id             BIGINT       REFERENCES zones(id) ON DELETE SET NULL,
    event_date          TIMESTAMP,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    instagram_post_id   VARCHAR(50)  UNIQUE,
    reviewed_by_id      UUID         REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at         TIMESTAMP,
    rejection_reason    VARCHAR(500),
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,

    CONSTRAINT chk_event_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_events_status       ON events(status);
CREATE INDEX idx_events_event_date   ON events(event_date);
CREATE INDEX idx_events_instagram_id ON events(instagram_post_id);

-- ── Imágenes del carrusel ─────────────────────────────────────────────────────
CREATE TABLE event_images (
    id                       BIGSERIAL     PRIMARY KEY,
    event_id                 UUID          NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    media_url                VARCHAR(1024) NOT NULL,
    source_instagram_post_id VARCHAR(50),
    display_order            INT           NOT NULL DEFAULT 0,
    created_at               TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_event_images_event_id       ON event_images(event_id);
CREATE INDEX idx_event_images_instagram_post ON event_images(source_instagram_post_id);

-- ── Recordatorios ─────────────────────────────────────────────────────────────
CREATE TABLE reminders (
    id             UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_id       UUID      NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    minutes_before INT       NOT NULL,
    sent           BOOLEAN   NOT NULL DEFAULT FALSE,
    sent_at        TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_reminders_user_event_minutes UNIQUE (user_id, event_id, minutes_before)
);

CREATE INDEX idx_reminders_user_id  ON reminders(user_id);
CREATE INDEX idx_reminders_event_id ON reminders(event_id);
CREATE INDEX idx_reminders_pending  ON reminders(sent) WHERE sent = FALSE;
