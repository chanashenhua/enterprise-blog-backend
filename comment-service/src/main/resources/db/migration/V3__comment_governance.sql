ALTER TABLE blog_comment DROP CONSTRAINT chk_blog_comment_status;
ALTER TABLE blog_comment
    ADD CONSTRAINT chk_blog_comment_status CHECK (status IN ('ACTIVE', 'HIDDEN', 'DELETED'));

CREATE INDEX idx_blog_comment_governance
    ON blog_comment(status, article_id, author_id, created_at DESC);

CREATE TABLE comment_audit_event (
    id VARCHAR(64) PRIMARY KEY,
    payload_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_comment_audit_event_status_created
    ON comment_audit_event(status, created_at);

COMMENT ON TABLE comment_audit_event IS '评论治理关键操作待投递审计事件';
COMMENT ON COLUMN blog_comment.status IS 'ACTIVE、HIDDEN 或 DELETED';
