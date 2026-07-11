-- ─────────────────────────────────────────────────────────────────────────────
-- V16: Agrega columna event_title_snapshot en audit_logs
--      para preservar el título del evento incluso después de eliminarlo
--      (ON DELETE SET NULL pone event_id en NULL, pero el título se pierde).
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS event_title_snapshot VARCHAR(500);

-- Poblar retroactivamente con los títulos de los eventos existentes
UPDATE audit_logs al
    SET event_title_snapshot = e.title
    FROM events e
    WHERE al.event_id = e.id
      AND al.event_title_snapshot IS NULL;
