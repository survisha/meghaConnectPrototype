ALTER TABLE appointments ADD COLUMN IF NOT EXISTS scheduled_by VARCHAR(100);
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS scheduled_at TIMESTAMP;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS rescheduled_by VARCHAR(100);
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS rescheduled_at TIMESTAMP;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS closed_by VARCHAR(100);
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS closed_at TIMESTAMP;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS final_remarks TEXT;

CREATE INDEX IF NOT EXISTS idx_appt_status_closed_at ON appointments(status, closed_at);
