-- ─────────────────────────────────────────────────────────────────────────────
-- V15: Dos cambios en audit_logs:
--   1. Ampliar CHECK constraint: incluir EDITED y DELETED
--   2. Cambiar event_id de ON DELETE CASCADE → ON DELETE SET NULL
--      para que los registros de auditoría de eventos ELIMINADOS persistan
--      (si fuera CASCADE, el registro de auditoría desaparecería junto con
--      el evento, anulando el propósito del registro de borrado).
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. Ampliar el CHECK
ALTER TABLE audit_logs
    DROP CONSTRAINT IF EXISTS chk_audit_action;

ALTER TABLE audit_logs
    ADD CONSTRAINT chk_audit_action
        CHECK (action IN ('APPROVED', 'REJECTED', 'EDITED', 'DELETED'));

-- 2. Cambiar el FK de event_id a ON DELETE SET NULL
ALTER TABLE audit_logs
    DROP CONSTRAINT IF EXISTS audit_logs_event_id_fkey;

ALTER TABLE audit_logs
    ALTER COLUMN event_id DROP NOT NULL;

ALTER TABLE audit_logs
    ADD CONSTRAINT audit_logs_event_id_fkey
        FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE SET NULL;

