package com.company.blog.article.api;

public interface ArticleSearchIndexClient {
    void index(String payloadJson);

    default void delete(String articleId) {
    }
}
