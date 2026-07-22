CREATE TABLE department (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL
);

CREATE TABLE team (
    id VARCHAR(64) PRIMARY KEY,
    department_id VARCHAR(64) NOT NULL REFERENCES department(id),
    name VARCHAR(128) NOT NULL
);

INSERT INTO department (id, name) VALUES
    ('d-platform', 'Platform Engineering'),
    ('d-pay', 'Payment Engineering');

INSERT INTO team (id, department_id, name) VALUES
    ('t-search', 'd-platform', 'Search Team'),
    ('t-pay', 'd-pay', 'Payment Team');
