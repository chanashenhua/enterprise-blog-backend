CREATE TABLE audit_record (
    id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(128) NOT NULL UNIQUE,
    source_service VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_roles VARCHAR(500) NOT NULL,
    action VARCHAR(80) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    details TEXT,
    trace_id VARCHAR(128),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_record_occurred ON audit_record(occurred_at DESC);
CREATE INDEX idx_audit_record_actor ON audit_record(actor_id, occurred_at DESC);
CREATE INDEX idx_audit_record_resource ON audit_record(resource_type, resource_id, occurred_at DESC);
CREATE INDEX idx_audit_record_action ON audit_record(action, occurred_at DESC);

COMMENT ON TABLE audit_record IS '关键管理操作审计记录';
COMMENT ON COLUMN audit_record.event_id IS '上游事件唯一标识，用于幂等去重';
COMMENT ON COLUMN audit_record.actor_id IS '操作人标识';
COMMENT ON COLUMN audit_record.action IS '标准化操作类型';
COMMENT ON COLUMN audit_record.resource_type IS '被操作资源类型';
COMMENT ON COLUMN audit_record.resource_id IS '被操作资源标识';
COMMENT ON COLUMN audit_record.occurred_at IS '业务操作实际发生时间';
