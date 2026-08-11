package com.company.blog.stats.api;

import com.company.blog.stats.InteractionType;

public interface InteractionRepository {
    boolean add(String articleId, String userId, InteractionType type);

    boolean remove(String articleId, String userId, InteractionType type);

    InteractionSnapshot snapshot(String articleId, String userId);
}
