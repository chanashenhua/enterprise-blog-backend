CREATE TABLE knowledge_collection (
    id VARCHAR(64) PRIMARY KEY,
    owner_id VARCHAR(64) NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge_collection_article (
    collection_id VARCHAR(64) NOT NULL REFERENCES knowledge_collection(id) ON DELETE CASCADE,
    article_id VARCHAR(64) NOT NULL REFERENCES article(id) ON DELETE CASCADE,
    position INTEGER NOT NULL CHECK (position >= 0),
    PRIMARY KEY (collection_id, article_id),
    UNIQUE (collection_id, position)
);

CREATE INDEX idx_knowledge_collection_owner_updated
    ON knowledge_collection(owner_id, updated_at DESC);
CREATE INDEX idx_knowledge_collection_updated
    ON knowledge_collection(updated_at DESC);

COMMENT ON TABLE knowledge_collection IS '员工维护的有序文章专题集合';
COMMENT ON TABLE knowledge_collection_article IS '专题集合中的文章及展示顺序';
