-- ─────────────────────────────────────────────────────────────────────────────
-- V7: Auditoría de acciones de revisión (aprobar/rechazar eventos)
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE audit_logs (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id      UUID         NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    supervisor_id UUID         REFERENCES users(id) ON DELETE SET NULL,
    action        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_audit_action CHECK (action IN ('APPROVED', 'REJECTED'))
);

CREATE INDEX idx_audit_logs_event_id   ON audit_logs(event_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
