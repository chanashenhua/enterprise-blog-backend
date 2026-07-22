package com.company.blog.permission.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.blog.permission.PermissionPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PermissionControllerWebTest {
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new PermissionController(new PermissionPolicy()))
            .build();

    @Test
    void exposesPermissionCheckJsonContract() throws Exception {
        mockMvc.perform(post("/internal/permissions/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "u-author",
                                  "roles": ["AUTHOR", "READER"],
                                  "departmentIds": ["d-platform"],
                                  "teamIds": ["t-search"],
                                  "action": "article.publish",
                                  "resourceType": "article",
                                  "resourceOwnerId": "u-author",
                                  "visibilityType": "company",
                                  "targetOrgIds": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.reason").value("ALLOWED"));
    }

    @Test
    void consumesGatewayUserContextHeaders() throws Exception {
        mockMvc.perform(post("/internal/permissions/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "u-author")
                        .header("X-User-Roles", "AUTHOR,READER")
                        .header("X-Department-Ids", "d-platform")
                        .header("X-Team-Ids", "t-search")
                        .content("""
                                {
                                  "action": "article.publish",
                                  "resourceType": "article",
                                  "resourceOwnerId": "u-author",
                                  "visibilityType": "company",
                                  "targetOrgIds": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.reason").value("ALLOWED"));
    }
}
