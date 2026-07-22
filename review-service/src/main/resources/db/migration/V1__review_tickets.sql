CREATE TABLE review_ticket (
    id VARCHAR(64) PRIMARY KEY,
    article_id VARCHAR(64) NOT NULL,
    author_id VARCHAR(64) NOT NULL,
    visibility_type VARCHAR(32) NOT NULL,
    target_org_ids TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_review_ticket_article_status ON review_ticket(article_id, status);