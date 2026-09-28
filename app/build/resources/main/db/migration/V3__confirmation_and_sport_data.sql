CREATE TABLE IF NOT EXISTS game_change_confirmations (
    change_id UUID NOT NULL REFERENCES game_changes(id), user_id UUID NOT NULL REFERENCES users(id), confirmed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), PRIMARY KEY(change_id,user_id)
);
CREATE TABLE IF NOT EXISTS facility_sport_associations (
    facility_id UUID NOT NULL REFERENCES facilities(id), sport TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'APPROVED', created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), PRIMARY KEY(facility_id,sport)
);
CREATE TABLE IF NOT EXISTS facility_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), suggested_by UUID NOT NULL REFERENCES users(id), facility_id UUID REFERENCES facilities(id), name TEXT NOT NULL, town TEXT NOT NULL, latitude DOUBLE PRECISION, longitude DOUBLE PRECISION,
    status TEXT NOT NULL DEFAULT 'SUGGESTED', created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE IF NOT EXISTS trust_recalculation_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), subject_id UUID NOT NULL, status TEXT NOT NULL DEFAULT 'PENDING', attempts INTEGER NOT NULL DEFAULT 0, available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), completed_at TIMESTAMPTZ
);
