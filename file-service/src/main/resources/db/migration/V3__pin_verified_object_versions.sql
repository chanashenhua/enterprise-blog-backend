ALTER TABLE file_metadata
    ADD COLUMN verified_version_id VARCHAR(256); -- 通过校验的 MinIO 对象版本标识

UPDATE file_metadata
SET upload_verified = FALSE
WHERE verified_version_id IS NULL;
