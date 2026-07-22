CREATE TABLE tag (
    id VARCHAR(64) PRIMARY KEY, -- 标签唯一标识
    name VARCHAR(80) NOT NULL UNIQUE, -- 标签显示名称
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP -- 标签创建时间
);

INSERT INTO tag (id, name) VALUES
    ('java', 'Java'),
    ('spring-cloud', 'Spring Cloud'),
    ('redis', 'Redis'),
    ('postgresql', 'PostgreSQL'),
    ('elasticsearch', 'Elasticsearch');
