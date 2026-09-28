ALTER TABLE disputes ADD COLUMN IF NOT EXISTS resolution TEXT;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS resolved_by UUID REFERENCES users(id);
CREATE INDEX IF NOT EXISTS idx_disputes_game_status ON disputes(game_id,status,created_at);
