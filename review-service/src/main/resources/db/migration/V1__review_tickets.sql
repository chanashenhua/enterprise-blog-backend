CREATE TABLE review_ticket (
    id VARCHAR(64) PRIMARY KEY, -- 审核单唯一标识
    article_id VARCHAR(64) NOT NULL, -- 待审核文章标识
    author_id VARCHAR(64) NOT NULL, -- 提交审核的文章作者标识
    visibility_type VARCHAR(32) NOT NULL, -- 申请发布的可见范围类型
    target_org_ids TEXT NOT NULL, -- 目标部门或团队标识集合，逗号分隔保存
    status VARCHAR(32) NOT NULL, -- 审核状态：PENDING、APPROVING、APPROVED、REJECTING、REJECTED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 审核单创建时间
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP -- 审核单状态最后更新时间
);

CREATE INDEX idx_review_ticket_article_status ON review_ticket(article_id, status);
