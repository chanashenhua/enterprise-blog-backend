package com.company.blog.notification.api;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SubscriptionNotificationService {
    private final ContentSubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;

    public SubscriptionNotificationService(
            ContentSubscriptionRepository subscriptionRepository,
            NotificationService notificationService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ArticlePublishedNotificationResult notifySubscribers(ArticlePublishedNotificationRequest request) {
        requireRequest(request);
        Set<String> recipients = new LinkedHashSet<>(
                subscriptionRepository.findSubscriberUserIds(request.categoryId(), request.tagIds())
        );
        recipients.remove(request.authorId());
        for (String recipient : recipients) {
            notificationService.create(new CreateNotificationRequest(
                    request.eventId().trim() + ":" + recipient,
                    recipient,
                    "SUBSCRIPTION_ARTICLE_PUBLISHED",
                    "你订阅的主题有新文章",
                    request.title().trim(),
                    "ARTICLE",
                    request.articleId().trim()
            ));
        }
        return new ArticlePublishedNotificationResult(recipients.size());
    }

    private static void requireRequest(ArticlePublishedNotificationRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Published article event is required");
        }
        requireText(request.eventId(), "Event id is required");
        requireText(request.articleId(), "Article id is required");
        requireText(request.authorId(), "Author id is required");
        requireText(request.title(), "Article title is required");
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }
}
