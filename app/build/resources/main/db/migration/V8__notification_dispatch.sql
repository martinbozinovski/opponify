ALTER TABLE domain_events ADD COLUMN IF NOT EXISTS notification_dispatched_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_domain_events_notification ON domain_events(notification_dispatched_at,occurred_at) WHERE notification_dispatched_at IS NULL;
