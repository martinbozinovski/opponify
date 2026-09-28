CREATE TABLE IF NOT EXISTS game_cancellations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), game_id UUID NOT NULL REFERENCES scheduled_games(id),
    cancelled_by UUID NOT NULL REFERENCES users(id), category TEXT NOT NULL, reason TEXT, cancelled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    effective_late BOOLEAN NOT NULL DEFAULT FALSE, excused BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_game_cancellations_game ON game_cancellations(game_id,cancelled_at);
