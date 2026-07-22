CREATE TABLE blog_user (
    id VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(128) NOT NULL
);

CREATE TABLE user_role (
    user_id VARCHAR(64) NOT NULL REFERENCES blog_user(id),
    role_code VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, role_code)
);

CREATE TABLE user_org_membership (
    user_id VARCHAR(64) NOT NULL REFERENCES blog_user(id),
    department_id VARCHAR(64) NOT NULL,
    team_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, department_id, team_id)
);

INSERT INTO blog_user (id, display_name) VALUES
    ('u-admin', 'Platform Admin'),
    ('u-author', 'Tech Author'),
    ('u-reader', 'Reader');

INSERT INTO user_role (user_id, role_code) VALUES
    ('u-admin', 'ADMIN'),
    ('u-admin', 'REVIEWER'),
    ('u-admin', 'AUTHOR'),
    ('u-admin', 'READER'),
    ('u-author', 'AUTHOR'),
    ('u-author', 'READER'),
    ('u-reader', 'READER');

INSERT INTO user_org_membership (user_id, department_id, team_id) VALUES
    ('u-admin', 'd-platform', 't-search'),
    ('u-author', 'd-platform', 't-search'),
    ('u-reader', 'd-pay', 't-pay');
