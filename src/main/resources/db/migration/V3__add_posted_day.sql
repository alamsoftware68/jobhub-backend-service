-- Calendar day a job was posted, so the list can be sorted day by day
-- (newest day first, best match first within the same day).
ALTER TABLE job ADD COLUMN posted_day DATE NULL AFTER posted_at;
UPDATE job SET posted_day = DATE(posted_at) WHERE posted_at IS NOT NULL;
CREATE INDEX idx_job_posted_day ON job (posted_day, match_score);
