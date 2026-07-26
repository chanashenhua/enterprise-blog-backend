ALTER TABLE article ADD COLUMN category_id VARCHAR(64);
ALTER TABLE article_content_version ADD COLUMN category_id VARCHAR(64);

CREATE INDEX idx_article_category
    ON article(category_id, updated_at DESC);

COMMENT ON COLUMN article.category_id IS '文章当前分类标识，由目录服务校验';
COMMENT ON COLUMN article_content_version.category_id IS '该内容版本的分类标识快照';
