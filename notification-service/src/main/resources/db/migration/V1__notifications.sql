CREATE TABLE user_notification (
    id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(128) NOT NULL UNIQUE,
    recipient_user_id VARCHAR(64) NOT NULL,
    notification_type VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    resource_type VARCHAR(64),
    resource_id VARCHAR(64),
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_notification_recipient_created
    ON user_notification(recipient_user_id, created_at DESC);

CREATE INDEX idx_user_notification_recipient_unread
    ON user_notification(recipient_user_id, read_at);

COMMENT ON TABLE user_notification IS '员工站内通知';
COMMENT ON COLUMN user_notification.event_id IS '上游事件幂等标识';
COMMENT ON COLUMN user_notification.recipient_user_id IS '通知接收员工标识';
COMMENT ON COLUMN user_notification.notification_type IS '通知业务类型';
COMMENT ON COLUMN user_notification.title IS '通知标题';
COMMENT ON COLUMN user_notification.content IS '通知摘要';
COMMENT ON COLUMN user_notification.resource_type IS '关联资源类型';
COMMENT ON COLUMN user_notification.resource_id IS '关联资源标识';
COMMENT ON COLUMN user_notification.read_at IS '首次标记已读时间';
COMMENT ON COLUMN user_notification.created_at IS '通知创建时间';
