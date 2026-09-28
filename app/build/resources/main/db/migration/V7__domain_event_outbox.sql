ALTER TABLE domain_events ADD COLUMN IF NOT EXISTS payload JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE domain_events ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_domain_events_unpublished ON domain_events(published_at,occurred_at) WHERE published_at IS NULL;
