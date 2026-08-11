package com.company.blog.article.api;

import java.util.List;

public interface ArticleEngagementClient {
    List<ArticleEngagement> topArticles(int limit);
}
