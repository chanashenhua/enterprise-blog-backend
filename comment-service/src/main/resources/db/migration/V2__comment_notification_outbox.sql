CREATE TABLE comment_notification_event (
    id VARCHAR(64) PRIMARY KEY,
    comment_id VARCHAR(64) NOT NULL UNIQUE REFERENCES blog_comment(id),
    payload_json TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_comment_notification_status
        CHECK (status IN ('PENDING', 'DELIVERED', 'FAILED'))
);

CREATE INDEX idx_comment_notification_status_created
    ON comment_notification_event(status, created_at);

COMMENT ON TABLE comment_notification_event IS '评论回复通知 Outbox';
COMMENT ON COLUMN comment_notification_event.comment_id IS '触发通知的回复评论标识';
COMMENT ON COLUMN comment_notification_event.payload_json IS '投递给通知服务的请求载荷';
COMMENT ON COLUMN comment_notification_event.status IS 'PENDING、DELIVERED 或 FAILED';
COMMENT ON COLUMN comment_notification_event.retry_count IS '远程投递失败次数';
