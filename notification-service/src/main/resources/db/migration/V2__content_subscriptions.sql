CREATE TABLE content_subscription (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_content_subscription_target UNIQUE (user_id, target_type, target_id),
    CONSTRAINT ck_content_subscription_type CHECK (target_type IN ('TAG', 'CATEGORY'))
);

CREATE INDEX idx_content_subscription_target
    ON content_subscription(target_type, target_id, user_id);

COMMENT ON TABLE content_subscription IS '员工对标签或分类的内容订阅关系';
COMMENT ON COLUMN content_subscription.target_type IS '订阅目标类型：TAG 或 CATEGORY';
COMMENT ON COLUMN content_subscription.target_id IS '标签或分类标识';
