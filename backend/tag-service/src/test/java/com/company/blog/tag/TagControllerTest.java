package com.company.blog.tag;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.blog.tag.api.TagController;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TagControllerTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new TagController()).build();

    @Test
    void validatesKnownEngineeringTags() throws Exception {
        mvc.perform(get("/internal/tags/validate")
                        .param("ids", "java")
                        .param("ids", "missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.validIds[0]").value("java"))
                .andExpect(jsonPath("$.unknownIds[0]").value("missing"));
    }
}