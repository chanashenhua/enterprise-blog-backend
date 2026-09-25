package com.company.blog.org;

import java.util.List;

public record OrgDirectory(List<Department> departments, List<Team> teams) {
    public record Department(String id, String name) {}
    public record Team(String id, String name, String departmentId, String departmentName) {}
}
