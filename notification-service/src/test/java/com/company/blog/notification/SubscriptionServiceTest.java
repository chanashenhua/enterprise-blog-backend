package com.company.blog.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.blog.notification.api.ArticlePublishedNotificationRequest;
import com.company.blog.notification.api.ContentSubscription;
import com.company.blog.notification.api.ContentSubscriptionRepository;
import com.company.blog.notification.api.NotificationRepository;
import com.company.blog.notification.api.NotificationService;
import com.company.blog.notification.api.SubscriptionNotificationService;
import com.company.blog.notification.api.SubscriptionService;
import com.company.blog.notification.api.SubscriptionTargetType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class SubscriptionServiceTest {
    @Test
    void subscribesIdempotentlyAndRejectsUnknownCatalogTargets() {
        InMemorySubscriptionRepository repository = new InMemorySubscriptionRepository();
        SubscriptionService service = new SubscriptionService(repository, (type, id) -> "java".equals(id));

        ContentSubscription first = service.subscribe("u-reader", "tag", "java");
        ContentSubscription duplicate = service.subscribe("u-reader", "TAG", "java");

        assertThat(duplicate.id()).isEqualTo(first.id());
        assertThat(service.list("u-reader")).hasSize(1);
        assertThatThrownBy(() -> service.subscribe("u-reader", "TAG", "missing"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        service.unsubscribe("u-reader", "TAG", "java");
        assertThat(service.list("u-reader")).isEmpty();
    }

    @Test
    void notifiesMatchingSubscribersOnceAndExcludesTheAuthor() {
        InMemorySubscriptionRepository subscriptions = new InMemorySubscriptionRepository();
        SubscriptionService subscriptionService = new SubscriptionService(subscriptions, (type, id) -> true);
        subscriptionService.subscribe("u-reader", "TAG", "java");
        subscriptionService.subscribe("u-reader", "CATEGORY", "backend");
        subscriptionService.subscribe("u-author", "TAG", "java");
        InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();
        SubscriptionNotificationService service = new SubscriptionNotificationService(
                subscriptions,
                new NotificationService(notifications)
        );
        ArticlePublishedNotificationRequest request = new ArticlePublishedNotificationRequest(
                "event-1", "article-1", "u-author", "Java 17 实践", "backend", Set.of("java")
        );

        assertThat(service.notifySubscribers(request).recipientCount()).isEqualTo(1);
        assertThat(service.notifySubscribers(request).recipientCount()).isEqualTo(1);
        assertThat(notifications.values).hasSize(1);
        assertThat(notifications.values.values().iterator().next().recipientUserId()).isEqualTo("u-reader");
    }

    private static final class InMemorySubscriptionRepository implements ContentSubscriptionRepository {
        private final List<ContentSubscription> values = new ArrayList<>();

        @Override
        public ContentSubscription save(ContentSubscription subscription) {
            return find(subscription.userId(), subscription.targetType(), subscription.targetId())
                    .orElseGet(() -> {
                        values.add(subscription);
                        return subscription;
                    });
        }

        @Override
        public Optional<ContentSubscription> find(String userId, SubscriptionTargetType type, String targetId) {
            return values.stream().filter(value -> value.userId().equals(userId)
                    && value.targetType() == type && value.targetId().equals(targetId)).findFirst();
        }

        @Override
        public List<ContentSubscription> findByUser(String userId) {
            return values.stream().filter(value -> value.userId().equals(userId)).toList();
        }

        @Override
        public void delete(String userId, SubscriptionTargetType type, String targetId) {
            values.removeIf(value -> value.userId().equals(userId)
                    && value.targetType() == type && value.targetId().equals(targetId));
        }

        @Override
        public Set<String> findSubscriberUserIds(String categoryId, Set<String> tagIds) {
            Set<String> users = new LinkedHashSet<>();
            values.stream().filter(value -> (value.targetType() == SubscriptionTargetType.CATEGORY
                            && value.targetId().equals(categoryId))
                            || (value.targetType() == SubscriptionTargetType.TAG && tagIds.contains(value.targetId())))
                    .map(ContentSubscription::userId)
                    .forEach(users::add);
            return users;
        }
    }

    private static final class InMemoryNotificationRepository implements NotificationRepository {
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
            return values.values().stream().filter(value -> value.recipientUserId().equals(recipientUserId)).toList();
        }

        @Override
        public long countUnread(String recipientUserId) {
            return findByRecipient(recipientUserId, 100).size();
        }

        @Override
        public Optional<Notification> markRead(String id, String recipientUserId) {
            return Optional.empty();
        }

        @Override
        public int markAllRead(String recipientUserId) {
            return 0;
        }
    }
}
