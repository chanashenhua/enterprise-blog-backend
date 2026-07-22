CREATE TABLE file_metadata (
    id VARCHAR(64) PRIMARY KEY, -- 文件唯一标识
    owner_id VARCHAR(64) NOT NULL, -- 上传文件的用户标识
    object_key VARCHAR(512) NOT NULL UNIQUE, -- MinIO 中对象的唯一存储键
    original_name VARCHAR(512) NOT NULL, -- 用户上传时的原始文件名
    content_type VARCHAR(128) NOT NULL, -- 允许的文件 MIME 类型
    size_bytes BIGINT NOT NULL, -- 文件声明且已核验的字节大小
    created_at TIMESTAMP WITH TIME ZONE NOT NULL -- 上传凭证创建时间
);

CREATE INDEX idx_file_metadata_owner ON file_metadata(owner_id);

CREATE TABLE file_binding (
    file_id VARCHAR(64) NOT NULL REFERENCES file_metadata(id) ON DELETE CASCADE, -- 关联文件标识
    resource_type VARCHAR(64) NOT NULL, -- 业务资源类型，例如 article
    resource_id VARCHAR(64) NOT NULL, -- 业务资源唯一标识
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 关联创建时间
    PRIMARY KEY (file_id, resource_type, resource_id)
);

CREATE INDEX idx_file_binding_resource ON file_binding(resource_type, resource_id);
