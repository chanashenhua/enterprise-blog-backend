package com.company.blog.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.blog.notification.api.CreateNotificationRequest;
import com.company.blog.notification.api.InternalNotificationController;
import com.company.blog.notification.api.NotificationController;
import com.company.blog.notification.api.NotificationResponse;
import com.company.blog.notification.api.NotificationService;
import com.company.blog.notification.api.UnreadCountResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NotificationControllerTest {
    private NotificationService service;
    private MockMvc publicMvc;
    private MockMvc internalMvc;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(NotificationService.class);
        publicMvc = MockMvcBuilders.standaloneSetup(new NotificationController(service)).build();
        internalMvc = MockMvcBuilders.standaloneSetup(
                new InternalNotificationController(service, "internal-token")
        ).build();
    }

    @Test
    void returnsTheCurrentUsersInboxAndUnreadCount() throws Exception {
        when(service.list("u-author", 50)).thenReturn(List.of(
                new NotificationResponse("n-1", "REVIEW_APPROVED", "审核通过", "文章已发布",
                        "ARTICLE", "a-1", false, Instant.now())
        ));
        when(service.unreadCount("u-author")).thenReturn(new UnreadCountResponse(1));

        publicMvc.perform(get("/api/notifications").header("X-User-Id", "u-author"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("n-1"));
        publicMvc.perform(get("/api/notifications/unread-count").header("X-User-Id", "u-author"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void protectsTheInternalCreationEndpoint() throws Exception {
        when(service.create(any(CreateNotificationRequest.class))).thenReturn(
                new NotificationResponse("n-1", "COMMENT_REPLY", "新回复", "收到回复",
                        "ARTICLE", "a-1", false, Instant.now())
        );
        String body = """
                {"eventId":"e-1","recipientUserId":"u-reader","type":"COMMENT_REPLY",
                 "title":"新回复","content":"收到回复","resourceType":"ARTICLE","resourceId":"a-1"}
                """;

        internalMvc.perform(post("/internal/notifications")
                        .header("X-Internal-Token", "wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
        internalMvc.perform(post("/internal/notifications")
                        .header("X-Internal-Token", "internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("n-1"));
    }
}
