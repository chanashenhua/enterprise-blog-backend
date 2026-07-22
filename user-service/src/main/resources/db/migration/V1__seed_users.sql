CREATE TABLE blog_user (
    id VARCHAR(64) PRIMARY KEY, -- 用户唯一标识
    display_name VARCHAR(128) NOT NULL -- 用户显示名称
);

CREATE TABLE user_role (
    user_id VARCHAR(64) NOT NULL REFERENCES blog_user(id), -- 用户标识
    role_code VARCHAR(64) NOT NULL, -- 角色编码：ADMIN、AUTHOR、REVIEWER、READER
    PRIMARY KEY (user_id, role_code)
);

CREATE TABLE user_org_membership (
    user_id VARCHAR(64) NOT NULL REFERENCES blog_user(id), -- 用户标识
    department_id VARCHAR(64) NOT NULL, -- 所属部门标识
    team_id VARCHAR(64) NOT NULL, -- 所属团队标识
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
