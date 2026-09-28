CREATE TABLE IF NOT EXISTS auth_identities (
    auth_subject TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deactivated_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_auth_identity_user ON auth_identities(user_id);
