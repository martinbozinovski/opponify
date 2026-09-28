ALTER TABLE trust_recalculation_jobs ADD COLUMN IF NOT EXISTS subject_type TEXT NOT NULL DEFAULT 'USER';
CREATE UNIQUE INDEX IF NOT EXISTS uq_pending_trust_job ON trust_recalculation_jobs(subject_id,subject_type) WHERE status IN ('PENDING','PROCESSING');
