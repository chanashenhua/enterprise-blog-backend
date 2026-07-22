ALTER TABLE review_ticket
    ADD CONSTRAINT uq_review_ticket_article UNIQUE (article_id);
