ALTER TABLE tag ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tag ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE category (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO category (id, name) VALUES
    ('architecture', '架构设计'),
    ('engineering', '工程实践'),
    ('database', '数据库'),
    ('operations', '运维与稳定性');

COMMENT ON TABLE category IS '文章分类目录';
COMMENT ON COLUMN category.id IS '分类唯一标识';
COMMENT ON COLUMN category.name IS '分类显示名称';
COMMENT ON COLUMN category.active IS '是否允许新文章使用';
COMMENT ON COLUMN tag.active IS '是否允许新文章使用';
