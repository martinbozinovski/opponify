CREATE TABLE IF NOT EXISTS team_membership_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), team_id UUID NOT NULL REFERENCES teams(id), user_id UUID NOT NULL REFERENCES users(id),
    status TEXT NOT NULL DEFAULT 'PENDING', expires_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(team_id,user_id)
);
CREATE INDEX IF NOT EXISTS idx_team_membership_requests_expiry ON team_membership_requests(status,expires_at);
