package com.company.blog.article.api;

import com.company.blog.article.domain.DomainEvent;
import java.util.List;

public interface ArticleOutbox {
    void appendArticleEvents(List<DomainEvent> events);

    default void appendArticleEvents(StoredArticle article, List<DomainEvent> events) {
        appendArticleEvents(events);
    }
}
