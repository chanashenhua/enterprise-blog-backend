CREATE TABLE tag_audit_event (
    id VARCHAR(64) PRIMARY KEY,
    payload_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tag_audit_event_pending
    ON tag_audit_event(status, created_at);

COMMENT ON TABLE tag_audit_event IS '分类和标签关键操作待投递审计事件';
