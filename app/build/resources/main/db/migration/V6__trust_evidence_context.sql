ALTER TABLE trust_evidence ADD COLUMN IF NOT EXISTS opponent_subject_id UUID;
ALTER TABLE trust_evidence ADD COLUMN IF NOT EXISTS metadata JSONB NOT NULL DEFAULT '{}'::jsonb;
CREATE INDEX IF NOT EXISTS idx_trust_evidence_subject_date ON trust_evidence(subject_id,occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_trust_evidence_opponent ON trust_evidence(subject_id,opponent_subject_id,occurred_at DESC);
