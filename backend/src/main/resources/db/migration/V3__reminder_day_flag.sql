-- Agrega el flag para rastrear el email del día del evento
-- independientemente del email de cuenta regresiva (sent).
ALTER TABLE reminders
    ADD COLUMN IF NOT EXISTS day_reminder_sent BOOLEAN NOT NULL DEFAULT FALSE;
