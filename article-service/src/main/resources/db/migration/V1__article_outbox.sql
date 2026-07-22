CREATE TABLE article (
    id VARCHAR(64) PRIMARY KEY, -- 文章唯一标识
    author_id VARCHAR(64) NOT NULL, -- 作者用户标识
    title VARCHAR(200) NOT NULL, -- 文章标题
    status VARCHAR(32) NOT NULL, -- 文章状态：DRAFT、PENDING_REVIEW、PUBLISHED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 文章创建时间
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP -- 文章最后更新时间
);

CREATE TABLE article_content (
    article_id VARCHAR(64) PRIMARY KEY REFERENCES article(id), -- 关联的文章标识
    content_json TEXT NOT NULL, -- 编辑器保存的原始 JSON 内容
    rendered_html TEXT NOT NULL, -- 经受控渲染和转义的展示 HTML
    plain_text TEXT NOT NULL -- 用于摘要和全文检索的纯文本内容
);

CREATE TABLE article_visibility_target (
    article_id VARCHAR(64) NOT NULL REFERENCES article(id), -- 关联的文章标识
    visibility_type VARCHAR(32) NOT NULL, -- 可见范围类型：DEPARTMENT 或 TEAM
    target_org_id VARCHAR(64) NOT NULL, -- 允许阅读的目标组织标识
    PRIMARY KEY (article_id, visibility_type, target_org_id)
);

CREATE TABLE article_publish_record (
    id VARCHAR(64) PRIMARY KEY, -- 发布记录唯一标识
    article_id VARCHAR(64) NOT NULL REFERENCES article(id), -- 被发布的文章标识
    published_by VARCHAR(64) NOT NULL, -- 执行发布操作的用户标识
    published_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP -- 文章发布时间
);

CREATE TABLE domain_event (
    id VARCHAR(64) PRIMARY KEY, -- 领域事件唯一标识
    aggregate_type VARCHAR(64) NOT NULL, -- 产生事件的聚合类型
    aggregate_id VARCHAR(64) NOT NULL, -- 产生事件的聚合标识
    event_type VARCHAR(100) NOT NULL, -- 事件类型，例如 ArticlePublished
    payload_json TEXT NOT NULL, -- 事件投递所需的 JSON 快照
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING', -- 投递状态：PENDING、DELIVERED、FAILED
    retry_count INTEGER NOT NULL DEFAULT 0, -- 累计投递重试次数
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 事件写入 Outbox 的时间
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP -- 事件状态最后更新时间
);

CREATE INDEX idx_article_author_status ON article(author_id, status);
CREATE INDEX idx_domain_event_status_created ON domain_event(status, created_at);
