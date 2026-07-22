\set ON_ERROR_STOP on

\connect user_db

INSERT INTO blog_user (id, display_name) VALUES
    ('u-admin', 'Platform Admin'),
    ('u-author', 'Tech Author'),
    ('u-reader', 'Reader')
ON CONFLICT (id) DO UPDATE SET display_name = EXCLUDED.display_name;

INSERT INTO user_role (user_id, role_code) VALUES
    ('u-admin', 'ADMIN'), ('u-admin', 'REVIEWER'), ('u-admin', 'AUTHOR'), ('u-admin', 'READER'),
    ('u-author', 'AUTHOR'), ('u-author', 'READER'), ('u-reader', 'READER')
ON CONFLICT DO NOTHING;

INSERT INTO user_org_membership (user_id, department_id, team_id) VALUES
    ('u-admin', 'd-platform', 't-search'),
    ('u-author', 'd-platform', 't-search'),
    ('u-reader', 'd-pay', 't-pay')
ON CONFLICT DO NOTHING;

\connect org_db

INSERT INTO department (id, name) VALUES
    ('d-platform', 'Platform Engineering'), ('d-pay', 'Payment Engineering')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO team (id, department_id, name) VALUES
    ('t-search', 'd-platform', 'Search Team'), ('t-pay', 'd-pay', 'Payment Team')
ON CONFLICT (id) DO UPDATE SET department_id = EXCLUDED.department_id, name = EXCLUDED.name;

\connect tag_db

INSERT INTO tag (id, name) VALUES
    ('java', 'Java'), ('spring-cloud', 'Spring Cloud'), ('redis', 'Redis'),
    ('postgresql', 'PostgreSQL'), ('elasticsearch', 'Elasticsearch')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;
