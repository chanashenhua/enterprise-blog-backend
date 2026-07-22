ALTER TABLE file_metadata
    ADD COLUMN verified_version_id VARCHAR(256);

UPDATE file_metadata
SET upload_verified = FALSE
WHERE verified_version_id IS NULL;
