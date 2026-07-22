package com.company.blog.article.api;

import com.company.blog.article.domain.Article;

public interface ReviewTicketClient {
    void createTicket(Article article, SubmitPublishRequest request);
}