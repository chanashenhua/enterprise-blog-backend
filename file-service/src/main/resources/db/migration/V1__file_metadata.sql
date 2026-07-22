CREATE TABLE file_metadata (
    id VARCHAR(64) PRIMARY KEY,
    owner_id VARCHAR(64) NOT NULL,
    object_key VARCHAR(512) NOT NULL UNIQUE,
    original_name VARCHAR(512) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_file_metadata_owner ON file_metadata(owner_id);

CREATE TABLE file_binding (
    file_id VARCHAR(64) NOT NULL REFERENCES file_metadata(id) ON DELETE CASCADE,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (file_id, resource_type, resource_id)
);

CREATE INDEX idx_file_binding_resource ON file_binding(resource_type, resource_id);
