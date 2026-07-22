ALTER TABLE review_ticket
    DROP CONSTRAINT uq_review_ticket_article;

ALTER TABLE review_ticket
    ADD COLUMN review_request_id VARCHAR(64); -- 一次发布提交的审核请求标识，用于回调幂等控制

UPDATE review_ticket
SET review_request_id = id
WHERE review_request_id IS NULL;

ALTER TABLE review_ticket
    ALTER COLUMN review_request_id SET NOT NULL;

ALTER TABLE review_ticket
    ADD CONSTRAINT uq_review_ticket_article_request UNIQUE (article_id, review_request_id);
