CREATE TABLE IF NOT EXISTS facilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name TEXT NOT NULL, town TEXT NOT NULL,
    status TEXT NOT NULL, latitude DOUBLE PRECISION, longitude DOUBLE PRECISION,
    public_precision TEXT NOT NULL DEFAULT 'FULL', created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS timezone TEXT;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS location_type TEXT NOT NULL DEFAULT 'TOWN';
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS free_form_location TEXT;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE opportunities ADD CONSTRAINT opportunities_creator_check CHECK ((creator_user_id IS NOT NULL) <> (creator_team_id IS NOT NULL));
CREATE INDEX IF NOT EXISTS idx_opportunities_expires ON opportunities(status, expires_at);

CREATE TABLE IF NOT EXISTS participation_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), opportunity_id UUID NOT NULL REFERENCES opportunities(id),
    requester_user_id UUID NULL REFERENCES users(id), requester_team_id UUID NULL REFERENCES teams(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), expires_at TIMESTAMPTZ, status TEXT NOT NULL,
    accepted_at TIMESTAMPTZ, updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK ((requester_user_id IS NOT NULL) <> (requester_team_id IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_requests_opportunity_status ON participation_requests(opportunity_id,status);

CREATE TABLE IF NOT EXISTS scheduled_games (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), opportunity_id UUID NOT NULL UNIQUE REFERENCES opportunities(id),
    start_at TIMESTAMPTZ NOT NULL, duration_seconds BIGINT NOT NULL CHECK(duration_seconds > 0), timezone TEXT NOT NULL,
    lifecycle TEXT NOT NULL DEFAULT 'SCHEDULED', created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_games_time ON scheduled_games(start_at, lifecycle);

CREATE TABLE IF NOT EXISTS game_participants (
    game_id UUID NOT NULL REFERENCES scheduled_games(id), participant_user_id UUID NULL REFERENCES users(id), participant_team_id UUID NULL REFERENCES teams(id),
    status TEXT NOT NULL DEFAULT 'ACTIVE', joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), exited_at TIMESTAMPTZ,
    PRIMARY KEY(game_id, participant_user_id, participant_team_id),
    CHECK ((participant_user_id IS NOT NULL) <> (participant_team_id IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_game_participants_game ON game_participants(game_id,status);

CREATE TABLE IF NOT EXISTS time_proposals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), game_id UUID NOT NULL REFERENCES scheduled_games(id),
    proposed_start_at TIMESTAMPTZ NOT NULL, proposer_user_id UUID REFERENCES users(id), proposer_team_id UUID REFERENCES teams(id),
    expires_at TIMESTAMPTZ NOT NULL, status TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), superseded_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_time_proposal ON time_proposals(game_id) WHERE status='PROPOSED';

CREATE TABLE IF NOT EXISTS time_proposal_confirmations (
    proposal_id UUID NOT NULL REFERENCES time_proposals(id), user_id UUID NOT NULL REFERENCES users(id), confirmed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY(proposal_id,user_id)
);

CREATE TABLE IF NOT EXISTS game_changes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), game_id UUID NOT NULL REFERENCES scheduled_games(id),
    change_type TEXT NOT NULL, previous_value JSONB NOT NULL, proposed_value JSONB NOT NULL,
    proposer_user_id UUID REFERENCES users(id), expires_at TIMESTAMPTZ NOT NULL, status TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_game_change ON game_changes(game_id) WHERE status='PROPOSED';

CREATE TABLE IF NOT EXISTS attendance_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), game_id UUID NOT NULL REFERENCES scheduled_games(id), participant_key TEXT NOT NULL,
    submitted_by UUID NOT NULL REFERENCES users(id), state TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(game_id,participant_key,submitted_by)
);
CREATE INDEX IF NOT EXISTS idx_attendance_game ON attendance_events(game_id,created_at);

CREATE TABLE IF NOT EXISTS results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), game_id UUID NOT NULL UNIQUE REFERENCES scheduled_games(id),
    state TEXT NOT NULL, payload JSONB, submitted_by UUID REFERENCES users(id), confirmed_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), game_id UUID REFERENCES scheduled_games(id), reporter_user_id UUID NOT NULL REFERENCES users(id),
    subject_type TEXT NOT NULL, subject_id UUID NOT NULL, reason TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), resolved_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS trust_evidence (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), subject_id UUID NOT NULL, subject_type TEXT NOT NULL,
    source_event_id UUID NOT NULL, evidence_type TEXT NOT NULL, severity INTEGER NOT NULL DEFAULT 0,
    occurred_at TIMESTAMPTZ NOT NULL, eligible BOOLEAN NOT NULL DEFAULT TRUE, methodology_version TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), UNIQUE(subject_id,source_event_id,evidence_type)
);
CREATE TABLE IF NOT EXISTS trust_assessments (
    subject_id UUID PRIMARY KEY, score INTEGER, status TEXT NOT NULL, methodology_version TEXT NOT NULL,
    assessed_at TIMESTAMPTZ NOT NULL, updating BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS blocks (
    blocker_user_id UUID NOT NULL REFERENCES users(id), blocked_user_id UUID NOT NULL REFERENCES users(id), created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY(blocker_user_id,blocked_user_id), CHECK(blocker_user_id<>blocked_user_id)
);
CREATE TABLE IF NOT EXISTS reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), reporter_user_id UUID NOT NULL REFERENCES users(id), subject_id UUID,
    category TEXT NOT NULL, description TEXT, status TEXT NOT NULL DEFAULT 'OPEN', created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), recipient_user_id UUID NOT NULL REFERENCES users(id), type TEXT NOT NULL,
    resource_id UUID, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), read_at TIMESTAMPTZ, payload JSONB
);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient_user_id,created_at DESC);

CREATE TABLE IF NOT EXISTS message_contexts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), game_id UUID NULL REFERENCES scheduled_games(id), user_a UUID NOT NULL REFERENCES users(id), user_b UUID NOT NULL REFERENCES users(id),
    active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), CHECK(user_a<>user_b)
);
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), context_id UUID NOT NULL REFERENCES message_contexts(id), sender_user_id UUID NOT NULL REFERENCES users(id),
    body TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), retained_until TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_messages_context ON messages(context_id,created_at);

CREATE TABLE IF NOT EXISTS idempotency_records_v2 (
    key TEXT NOT NULL, operation TEXT NOT NULL, actor_user_id UUID, response_status INTEGER, response_body JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), expires_at TIMESTAMPTZ NOT NULL, PRIMARY KEY(key,operation)
);

CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), actor_user_id UUID, action TEXT NOT NULL, resource_type TEXT NOT NULL,
    resource_id UUID, correlation_id TEXT, metadata JSONB, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
