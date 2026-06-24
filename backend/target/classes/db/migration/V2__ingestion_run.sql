-- ─────────────────────────────────────────────────────────────────────────────
-- V2: Registro de ejecuciones del job de ingesta de Instagram
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE ingestion_runs (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    started_at      TIMESTAMP    NOT NULL,
    ended_at        TIMESTAMP,
    trigger_type    VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    status          VARCHAR(20)  NOT NULL DEFAULT 'RUNNING',
    created_count   INT          NOT NULL DEFAULT 0,
    merged_count    INT          NOT NULL DEFAULT 0,
    rejected_count  INT          NOT NULL DEFAULT 0,
    skipped_count   INT          NOT NULL DEFAULT 0,
    error_message   VARCHAR(500),

    CONSTRAINT chk_ingestion_run_status CHECK (status       IN ('RUNNING', 'SUCCESS', 'FAILED')),
    CONSTRAINT chk_ingestion_trigger    CHECK (trigger_type IN ('SCHEDULED', 'MANUAL'))
);

CREATE INDEX idx_ingestion_runs_started_at ON ingestion_runs(started_at DESC);
CREATE INDEX idx_ingestion_runs_status     ON ingestion_runs(status);
