CREATE TABLE IF NOT EXISTS opportunity_changes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), opportunity_id UUID NOT NULL REFERENCES opportunities(id), change_type TEXT NOT NULL,
    previous_value JSONB NOT NULL, proposed_value JSONB NOT NULL, proposer_user_id UUID REFERENCES users(id), expires_at TIMESTAMPTZ NOT NULL,
    status TEXT NOT NULL DEFAULT 'PROPOSED', created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_opportunity_change ON opportunity_changes(opportunity_id) WHERE status='PROPOSED';
CREATE TABLE IF NOT EXISTS opportunity_change_confirmations (
    change_id UUID NOT NULL REFERENCES opportunity_changes(id), user_id UUID NOT NULL REFERENCES users(id), confirmed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), PRIMARY KEY(change_id,user_id)
);
