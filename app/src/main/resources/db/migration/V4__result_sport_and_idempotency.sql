ALTER TABLE results ADD COLUMN IF NOT EXISTS sport TEXT;
ALTER TABLE results ADD COLUMN IF NOT EXISTS schema_version INTEGER NOT NULL DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_results_game_state ON results(game_id,state);
