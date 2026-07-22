ALTER TABLE file_metadata
    ADD COLUMN upload_verified BOOLEAN NOT NULL DEFAULT FALSE; -- 对象是否已完成大小、类型和内容校验
