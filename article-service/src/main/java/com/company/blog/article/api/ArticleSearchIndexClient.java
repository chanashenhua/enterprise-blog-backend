package com.company.blog.article.api;

public interface ArticleSearchIndexClient {
    void index(String payloadJson);

    void delete(String articleId, long revision);
}
