CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE TABLE IF NOT EXISTS scheduled_commitment_intervals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), game_id UUID NOT NULL REFERENCES scheduled_games(id), commitment_key TEXT NOT NULL,
    start_at TIMESTAMPTZ NOT NULL, end_at TIMESTAMPTZ NOT NULL CHECK(end_at>start_at),
    EXCLUDE USING gist (commitment_key WITH =, tstzrange(start_at,end_at,'[)') WITH &&)
);
CREATE INDEX IF NOT EXISTS idx_commitment_intervals_game ON scheduled_commitment_intervals(game_id);
