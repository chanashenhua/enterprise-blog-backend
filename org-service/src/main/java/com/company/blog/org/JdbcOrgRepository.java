package com.company.blog.org;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOrgRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcOrgRepository(JdbcTemplate template) {
        this.jdbc = new NamedParameterJdbcTemplate(template);
    }

    public OrgDirectory directory(boolean admin, Set<String> departmentIds, Set<String> teamIds) {
        List<OrgDirectory.Department> departments = !admin && departmentIds.isEmpty() ? List.of() : jdbc.query(
                "select id, name from department" + (admin ? "" : " where id in (:ids)") + " order by name, id",
                Map.of("ids", departmentIds), (rs, row) -> new OrgDirectory.Department(rs.getString("id"), rs.getString("name")));
        List<OrgDirectory.Team> teams = !admin && teamIds.isEmpty() ? List.of() : jdbc.query(
                "select t.id, t.name, t.department_id, d.name as department_name from team t join department d on d.id = t.department_id"
                        + (admin ? "" : " where t.id in (:ids)") + " order by d.name, t.name, t.id",
                Map.of("ids", teamIds), (rs, row) -> new OrgDirectory.Team(rs.getString("id"), rs.getString("name"),
                        rs.getString("department_id"), rs.getString("department_name")));
        return new OrgDirectory(departments, teams);
    }

    public boolean allExist(String type, Set<String> ids) {
        // 表名只来自服务端固定枚举，所有用户输入通过绑定参数传入。
        String table = switch (type) { case "DEPARTMENT" -> "department"; case "TEAM" -> "team"; default -> throw new IllegalArgumentException("Unsupported org type"); };
        if (ids.isEmpty()) return false;
        Integer count = jdbc.queryForObject("select count(*) from " + table + " where id in (:ids)", Map.of("ids", ids), Integer.class);
        return count != null && count == ids.size();
    }
}
