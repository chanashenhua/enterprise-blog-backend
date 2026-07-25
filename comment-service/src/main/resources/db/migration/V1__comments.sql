CREATE TABLE blog_comment (
    id VARCHAR(64) PRIMARY KEY,
    article_id VARCHAR(64) NOT NULL,
    parent_id VARCHAR(64) REFERENCES blog_comment(id),
    author_id VARCHAR(64) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_blog_comment_status CHECK (status IN ('ACTIVE', 'DELETED')),
    CONSTRAINT chk_blog_comment_parent CHECK (parent_id IS NULL OR parent_id <> id)
);

CREATE INDEX idx_blog_comment_article_created
    ON blog_comment(article_id, created_at, id);

CREATE INDEX idx_blog_comment_parent
    ON blog_comment(parent_id);

COMMENT ON TABLE blog_comment IS '文章评论及一层回复';
COMMENT ON COLUMN blog_comment.article_id IS '评论所属文章标识';
COMMENT ON COLUMN blog_comment.parent_id IS '回复的根评论标识，根评论为空';
COMMENT ON COLUMN blog_comment.author_id IS '评论作者标识';
COMMENT ON COLUMN blog_comment.content IS '评论正文，软删除后清空';
COMMENT ON COLUMN blog_comment.status IS 'ACTIVE 或 DELETED';
