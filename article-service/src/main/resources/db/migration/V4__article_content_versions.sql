CREATE TABLE article_content_version (
    article_id VARCHAR(64) NOT NULL REFERENCES article(id) ON DELETE CASCADE,
    version_no INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    content_json TEXT NOT NULL,
    rendered_html TEXT NOT NULL,
    plain_text TEXT NOT NULL,
    tag_ids TEXT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (article_id, version_no)
);

CREATE INDEX idx_article_content_version_created
    ON article_content_version(article_id, created_at DESC);

COMMENT ON TABLE article_content_version IS '文章草稿每次创建或编辑形成的不可变内容版本';
COMMENT ON COLUMN article_content_version.article_id IS '关联的文章标识';
COMMENT ON COLUMN article_content_version.version_no IS '文章内从 1 开始递增的版本号';
COMMENT ON COLUMN article_content_version.title IS '该版本的文章标题';
COMMENT ON COLUMN article_content_version.content_json IS '该版本的编辑器原始 JSON';
COMMENT ON COLUMN article_content_version.rendered_html IS '该版本的受控渲染 HTML';
COMMENT ON COLUMN article_content_version.plain_text IS '该版本的纯文本投影';
COMMENT ON COLUMN article_content_version.tag_ids IS '该版本标签标识的逗号分隔快照';
COMMENT ON COLUMN article_content_version.created_by IS '创建该版本的用户标识';
COMMENT ON COLUMN article_content_version.created_at IS '该版本的创建时间';
