package com.company.blog.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.blog.notification.api.CreateNotificationRequest;
import com.company.blog.notification.api.NotificationRepository;
import com.company.blog.notification.api.NotificationResponse;
import com.company.blog.notification.api.NotificationService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotificationServiceTest {
    @Test
    void createsIdempotentNotificationsAndTracksReadStatePerRecipient() {
        InMemoryRepository repository = new InMemoryRepository();
        NotificationService service = new NotificationService(repository);
        CreateNotificationRequest request = new CreateNotificationRequest(
                "comment-reply:c-1",
                "u-reader",
                "COMMENT_REPLY",
                "收到新回复",
                "作者回复了你的评论",
                "ARTICLE",
                "a-1"
        );

        NotificationResponse first = service.create(request);
        NotificationResponse duplicate = service.create(request);

        assertThat(duplicate.id()).isEqualTo(first.id());
        assertThat(service.unreadCount("u-reader").count()).isEqualTo(1);
        assertThat(service.list("u-reader", 50)).hasSize(1);
        assertThat(service.markRead("u-reader", first.id()).read()).isTrue();
        assertThat(service.unreadCount("u-reader").count()).isZero();
    }

    @Test
    void doesNotAllowAUserToReadAnotherUsersNotification() {
        NotificationService service = new NotificationService(new InMemoryRepository());
        NotificationResponse notification = service.create(new CreateNotificationRequest(
                "review:r-1",
                "u-author",
                "REVIEW_APPROVED",
                "审核通过",
                "文章已经发布",
                "ARTICLE",
                "a-1"
        ));

        assertThatThrownBy(() -> service.markRead("u-reader", notification.id()))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
                );
    }

    private static final class InMemoryRepository implements NotificationRepository {
        private final Map<String, Notification> values = new LinkedHashMap<>();

        @Override
        public Notification save(Notification notification) {
            return findByEventId(notification.eventId()).orElseGet(() -> {
                values.put(notification.id(), notification);
                return notification;
            });
        }

        @Override
        public Optional<Notification> findByEventId(String eventId) {
            return values.values().stream().filter(value -> value.eventId().equals(eventId)).findFirst();
        }

        @Override
        public List<Notification> findByRecipient(String recipientUserId, int limit) {
            return values.values().stream()
                    .filter(value -> value.recipientUserId().equals(recipientUserId))
                    .limit(limit)
                    .toList();
        }

        @Override
        public long countUnread(String recipientUserId) {
            return values.values().stream()
                    .filter(value -> value.recipientUserId().equals(recipientUserId) && !value.read())
                    .count();
        }

        @Override
        public Optional<Notification> markRead(String id, String recipientUserId) {
            return Optional.ofNullable(values.get(id))
                    .filter(value -> value.recipientUserId().equals(recipientUserId))
                    .map(value -> {
                        Notification read = replace(value, Instant.now());
                        values.put(id, read);
                        return read;
                    });
        }

        @Override
        public int markAllRead(String recipientUserId) {
            int changed = 0;
            for (Notification value : List.copyOf(values.values())) {
                if (value.recipientUserId().equals(recipientUserId) && !value.read()) {
                    values.put(value.id(), replace(value, Instant.now()));
                    changed++;
                }
            }
            return changed;
        }

        private static Notification replace(Notification value, Instant readAt) {
            return new Notification(
                    value.id(),
                    value.eventId(),
                    value.recipientUserId(),
                    value.type(),
                    value.title(),
                    value.content(),
                    value.resourceType(),
                    value.resourceId(),
                    readAt,
                    value.createdAt()
            );
        }
    }
}
