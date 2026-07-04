-- ─────────────────────────────────────────────────────────────────────────────
-- V9: Almacena el access token de Instagram en BD (en vez de solo en el YAML)
-- para poder renovarlo automáticamente sin reiniciar el backend. Fila única
-- (id=1) — se autosiembra desde app.instagram.access-token la primera vez
-- que se usa (ver InstagramTokenProvider), no hay ningún secreto en este SQL.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE instagram_token (
    id           INT          PRIMARY KEY,
    access_token VARCHAR(500) NOT NULL,
    expires_at   TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_instagram_token_single_row CHECK (id = 1)
);
