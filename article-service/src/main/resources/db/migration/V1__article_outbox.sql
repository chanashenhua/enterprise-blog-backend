CREATE TABLE article (
    id VARCHAR(64) PRIMARY KEY,
    author_id VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE article_content (
    article_id VARCHAR(64) PRIMARY KEY REFERENCES article(id),
    content_json TEXT NOT NULL,
    rendered_html TEXT NOT NULL,
    plain_text TEXT NOT NULL
);

CREATE TABLE article_visibility_target (
    article_id VARCHAR(64) NOT NULL REFERENCES article(id),
    visibility_type VARCHAR(32) NOT NULL,
    target_org_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (article_id, visibility_type, target_org_id)
);

CREATE TABLE article_publish_record (
    id VARCHAR(64) PRIMARY KEY,
    article_id VARCHAR(64) NOT NULL REFERENCES article(id),
    published_by VARCHAR(64) NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE domain_event (
    id VARCHAR(64) PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_article_author_status ON article(author_id, status);
CREATE INDEX idx_domain_event_status_created ON domain_event(status, created_at);