CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_subject TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deactivated_at TIMESTAMPTZ NULL
);

CREATE TABLE IF NOT EXISTS player_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    skill_level TEXT NULL,
    public_profile BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS team_memberships (
    team_id UUID NOT NULL REFERENCES teams(id),
    user_id UUID NOT NULL REFERENCES users(id),
    role TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (team_id, user_id)
);

CREATE TABLE IF NOT EXISTS opportunities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_user_id UUID NULL REFERENCES users(id),
    creator_team_id UUID NULL REFERENCES teams(id),
    sport TEXT NOT NULL,
    need_type TEXT NOT NULL,
    time_type TEXT NOT NULL,
    start_at TIMESTAMPTZ NULL,
    end_at TIMESTAMPTZ NULL,
    town TEXT NULL,
    facility_id UUID NULL,
    target_capacity INTEGER NOT NULL CHECK (target_capacity > 0),
    minimum_participation INTEGER NOT NULL CHECK (minimum_participation > 0),
    skill_level TEXT NULL,
    desired_opponent_level TEXT NULL,
    status TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (minimum_participation <= target_capacity)
);

CREATE INDEX IF NOT EXISTS idx_opportunities_discovery
    ON opportunities(status, sport, town, start_at);

CREATE TABLE IF NOT EXISTS domain_events (
    event_id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    payload_version INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS idempotency_records (
    key TEXT PRIMARY KEY,
    operation TEXT NOT NULL,
    response_status INTEGER NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NULL
);
