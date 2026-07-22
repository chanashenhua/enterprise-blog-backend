COMMENT ON TABLE department IS '公司部门目录';
COMMENT ON COLUMN department.id IS '部门唯一标识';
COMMENT ON COLUMN department.name IS '部门名称';

COMMENT ON TABLE team IS '隶属于部门的团队目录';
COMMENT ON COLUMN team.id IS '团队唯一标识';
COMMENT ON COLUMN team.department_id IS '所属部门标识';
COMMENT ON COLUMN team.name IS '团队名称';
