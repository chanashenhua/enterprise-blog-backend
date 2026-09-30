ALTER TABLE article ADD COLUMN revision BIGINT NOT NULL DEFAULT 1;
ALTER TABLE article ADD CONSTRAINT article_revision_positive CHECK (revision > 0);

COMMENT ON COLUMN article.revision IS '文章修订号；实际内容或状态变化时递增，与历史快照版本号独立';
COMMENT ON TABLE article_content_version IS '手动创建、手动保存或提交发布时形成的不可变内容快照，自动保存不新增快照';
