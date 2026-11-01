-- Country of each job (English name, e.g. "Germany"), used by the Country filter
ALTER TABLE job ADD COLUMN country VARCHAR(100) NULL AFTER location;
CREATE INDEX idx_job_country ON job (country);

-- Jobs stored before this change: Arbeitsagentur jobs are always in Germany
UPDATE job SET country = 'Germany' WHERE source = 'ARBEITSAGENTUR';
