ALTER TABLE article ADD COLUMN visibility_type VARCHAR(32);
ALTER TABLE article ADD COLUMN review_request_id VARCHAR(64);
ALTER TABLE article ADD COLUMN approved_by_review_ticket_id VARCHAR(64);
ALTER TABLE article ADD COLUMN rejected_by_review_ticket_id VARCHAR(64);

CREATE TABLE article_tag (
    article_id VARCHAR(64) NOT NULL REFERENCES article(id) ON DELETE CASCADE,
    tag_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (article_id, tag_id)
);

CREATE INDEX idx_article_tag_tag_id ON article_tag(tag_id);

COMMENT ON COLUMN article.visibility_type IS '文章当前可见范围类型，如 COMPANY、DEPARTMENT 或 TEAM';
COMMENT ON COLUMN article.review_request_id IS '当前审核请求标识，用于过滤过期审核回调';
COMMENT ON COLUMN article.approved_by_review_ticket_id IS '最近一次成功发布文章的审核单标识';
COMMENT ON COLUMN article.rejected_by_review_ticket_id IS '最近一次退回文章的审核单标识';
COMMENT ON TABLE article_tag IS '文章与标签的关联关系';
COMMENT ON COLUMN article_tag.article_id IS '关联的文章标识';
COMMENT ON COLUMN article_tag.tag_id IS '标签服务维护的标签标识';
