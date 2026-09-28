CREATE TABLE IF NOT EXISTS facility_disruptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), game_id UUID NOT NULL REFERENCES scheduled_games(id), facility_id UUID REFERENCES facilities(id),
    reported_by UUID NOT NULL REFERENCES users(id), category TEXT NOT NULL, description TEXT, status TEXT NOT NULL DEFAULT 'OPEN', created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), resolved_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_facility_disruptions_game ON facility_disruptions(game_id,created_at);
