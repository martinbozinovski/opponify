ALTER TABLE trust_assessments ADD COLUMN IF NOT EXISTS subject_type TEXT NOT NULL DEFAULT 'USER';
ALTER TABLE trust_assessments DROP CONSTRAINT IF EXISTS trust_assessments_pkey;
ALTER TABLE trust_assessments ADD PRIMARY KEY(subject_id,subject_type);
