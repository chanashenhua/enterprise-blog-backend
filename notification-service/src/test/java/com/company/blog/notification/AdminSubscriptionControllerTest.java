package com.company.blog.notification;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.blog.notification.api.AdminSubscriptionController;
import com.company.blog.notification.api.SubscriptionGovernanceOverview;
import com.company.blog.notification.api.SubscriptionGovernanceService;
import com.company.blog.notification.api.SubscriptionTargetSummary;
import com.company.blog.notification.api.SubscriptionTargetType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminSubscriptionControllerTest {
    @Test
    void exposesOnlyAggregatedSubscriptionCoverageToAdmins() throws Exception {
        SubscriptionGovernanceService service = Mockito.mock(SubscriptionGovernanceService.class);
        when(service.overview()).thenReturn(new SubscriptionGovernanceOverview(
                3,
                2,
                2,
                1,
                List.of(new SubscriptionTargetSummary(SubscriptionTargetType.TAG, "java", 2))
        ));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminSubscriptionController(service)).build();

        mvc.perform(get("/api/admin/notifications/subscriptions/overview")
                        .header("X-User-Roles", "READER"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/admin/notifications/subscriptions/overview")
                        .header("X-User-Roles", "ADMIN,REVIEWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSubscriptionCount").value(3))
                .andExpect(jsonPath("$.subscriberCount").value(2))
                .andExpect(jsonPath("$.topTargets[0].targetId").value("java"));
    }
}
