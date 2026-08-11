package com.company.blog.article.api;

import java.util.List;

public interface FeedSubscriptionClient {
    List<FeedSubscription> findByUser(String userId);
}
