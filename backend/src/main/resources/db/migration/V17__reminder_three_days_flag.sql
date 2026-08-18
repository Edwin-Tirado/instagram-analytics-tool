-- Reemplaza el flag del email "día del evento a las 08:00" por el nuevo
-- recordatorio fijo de 3 días antes del evento (ver ReminderNotificationJob).
ALTER TABLE reminders
    RENAME COLUMN day_reminder_sent TO three_days_reminder_sent;
