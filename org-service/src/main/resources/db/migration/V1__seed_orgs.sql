CREATE TABLE department (
    id VARCHAR(64) PRIMARY KEY, -- 部门唯一标识
    name VARCHAR(128) NOT NULL -- 部门名称
);

CREATE TABLE team (
    id VARCHAR(64) PRIMARY KEY, -- 团队唯一标识
    department_id VARCHAR(64) NOT NULL REFERENCES department(id), -- 所属部门标识
    name VARCHAR(128) NOT NULL -- 团队名称
);

INSERT INTO department (id, name) VALUES
    ('d-platform', 'Platform Engineering'),
    ('d-pay', 'Payment Engineering');

INSERT INTO team (id, department_id, name) VALUES
    ('t-search', 'd-platform', 'Search Team'),
    ('t-pay', 'd-pay', 'Payment Team');
