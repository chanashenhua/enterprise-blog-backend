COMMENT ON TABLE blog_user IS '博客系统用户目录';
COMMENT ON COLUMN blog_user.id IS '用户唯一标识';
COMMENT ON COLUMN blog_user.display_name IS '用户显示名称';

COMMENT ON TABLE user_role IS '用户拥有的平台角色';
COMMENT ON COLUMN user_role.user_id IS '用户标识';
COMMENT ON COLUMN user_role.role_code IS '角色编码，如 ADMIN、AUTHOR、REVIEWER、READER';

COMMENT ON TABLE user_org_membership IS '用户所属的部门和团队关系';
COMMENT ON COLUMN user_org_membership.user_id IS '用户标识';
COMMENT ON COLUMN user_org_membership.department_id IS '所属部门标识';
COMMENT ON COLUMN user_org_membership.team_id IS '所属团队标识';
