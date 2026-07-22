CREATE TABLE tag (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO tag (id, name) VALUES
    ('java', 'Java'),
    ('spring-cloud', 'Spring Cloud'),
    ('redis', 'Redis'),
    ('postgresql', 'PostgreSQL'),
    ('elasticsearch', 'Elasticsearch');