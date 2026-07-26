CREATE TABLE article_interaction (
    article_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    interaction_type VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (article_id, user_id, interaction_type),
    CONSTRAINT chk_article_interaction_type
        CHECK (interaction_type IN ('VIEW', 'LIKE', 'FAVORITE'))
);

CREATE INDEX idx_article_interaction_article_type
    ON article_interaction(article_id, interaction_type);

CREATE INDEX idx_article_interaction_user_type
    ON article_interaction(user_id, interaction_type, created_at);

COMMENT ON TABLE article_interaction IS '员工对文章的去重浏览、点赞和收藏事实';
COMMENT ON COLUMN article_interaction.article_id IS '被互动的文章标识';
COMMENT ON COLUMN article_interaction.user_id IS '产生互动的员工标识';
COMMENT ON COLUMN article_interaction.interaction_type IS 'VIEW、LIKE 或 FAVORITE';
COMMENT ON COLUMN article_interaction.created_at IS '首次产生该互动的时间';
