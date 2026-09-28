CREATE TABLE IF NOT EXISTS opportunity_time_proposals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), opportunity_id UUID NOT NULL REFERENCES opportunities(id),
    proposed_start_at TIMESTAMPTZ NOT NULL, proposer_user_id UUID NOT NULL REFERENCES users(id), expires_at TIMESTAMPTZ NOT NULL,
    status TEXT NOT NULL DEFAULT 'PROPOSED', created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), confirmed_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_opportunity_time_proposal ON opportunity_time_proposals(opportunity_id) WHERE status='PROPOSED';
CREATE TABLE IF NOT EXISTS opportunity_time_confirmations (
    proposal_id UUID NOT NULL REFERENCES opportunity_time_proposals(id), user_id UUID NOT NULL REFERENCES users(id), confirmed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), PRIMARY KEY(proposal_id,user_id)
);
