package com.company.blog.stats.api;

public interface AdminInteractionRepository {
    AdminInteractionOverview overview(int topArticleLimit);
}
