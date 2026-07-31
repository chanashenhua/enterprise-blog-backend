package com.company.blog.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.blog.notification.api.AdminNotificationController;
import com.company.blog.notification.api.JdbcAdminNotificationRepository;
import com.company.blog.notification.api.JdbcNotificationRepository;
import com.company.blog.notification.api.NotificationGovernanceOverview;
import com.company.blog.notification.api.NotificationGovernanceQuery;
import com.company.blog.notification.api.NotificationGovernanceService;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NotificationGovernanceTest {
    private JdbcTemplate jdbcTemplate;
    private JdbcAdminNotificationRepository adminRepository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:notification_governance;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .cleanDisabled(false).load().clean();
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbcTemplate = new JdbcTemplate(dataSource);
        adminRepository = new JdbcAdminNotificationRepository(new NamedParameterJdbcTemplate(dataSource));

        JdbcNotificationRepository repository = new JdbcNotificationRepository(jdbcTemplate);
        repository.save(notification("n-1", "event-1", "u-author", "REVIEW_APPROVED"));
        repository.save(notification("n-2", "event-2", "u-author", "COMMENT_REPLY"));
        repository.save(notification("n-3", "event-3", "u-reader", "COMMENT_REPLY"));
        repository.markRead("n-2", "u-author");
    }

    @Test
    void summarizesAndFiltersNotificationDelivery() {
        NotificationGovernanceOverview overview = adminRepository.overview();
        assertThat(overview.totalCount()).isEqualTo(3);
        assertThat(overview.unreadCount()).isEqualTo(2);
        assertThat(overview.readCount()).isEqualTo(1);
        assertThat(overview.recipientCount()).isEqualTo(2);
        assertThat(overview.typeSummaries()).extracting(item -> item.type())
                .containsExactly("COMMENT_REPLY", "REVIEW_APPROVED");

        assertThat(adminRepository.search(new NotificationGovernanceQuery("u-author", null, false, 20)))
                .singleElement()
                .extracting(item -> item.id())
                .isEqualTo("n-1");
    }

    @Test
    void adminEndpointsProtectAccessAndValidateState() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new AdminNotificationController(new NotificationGovernanceService(adminRepository))
        ).build();

        mvc.perform(get("/api/admin/notifications/overview").header("X-User-Roles", "AUTHOR"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/notifications/overview").header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3));
        mvc.perform(get("/api/admin/notifications")
                        .header("X-User-Roles", "ADMIN")
                        .param("state", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    private static Notification notification(String id, String eventId, String recipient, String type) {
        return new Notification(
                id, eventId, recipient, type, "演示通知", "演示内容", "ARTICLE", "a-1", null, Instant.now()
        );
    }
}
