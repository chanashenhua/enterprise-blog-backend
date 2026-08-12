\set ON_ERROR_STOP on

-- 本脚本面向 localhost:5432/postgres 的单库 public schema，可安全重复执行。
INSERT INTO blog_user (id, display_name) VALUES
    ('u-admin', 'Platform Admin'),
    ('u-author', 'Tech Author'),
    ('u-reader', 'Reader')
ON CONFLICT (id) DO UPDATE SET display_name = EXCLUDED.display_name;

INSERT INTO user_role (user_id, role_code) VALUES
    ('u-admin', 'ADMIN'),
    ('u-admin', 'REVIEWER'),
    ('u-admin', 'AUTHOR'),
    ('u-admin', 'READER'),
    ('u-author', 'AUTHOR'),
    ('u-author', 'READER'),
    ('u-reader', 'READER')
ON CONFLICT DO NOTHING;

INSERT INTO department (id, name) VALUES
    ('d-platform', 'Platform Engineering'),
    ('d-pay', 'Payment Engineering')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO team (id, department_id, name) VALUES
    ('t-search', 'd-platform', 'Search Team'),
    ('t-pay', 'd-pay', 'Payment Team')
ON CONFLICT (id) DO UPDATE
SET department_id = EXCLUDED.department_id,
    name = EXCLUDED.name;

INSERT INTO user_org_membership (user_id, department_id, team_id) VALUES
    ('u-admin', 'd-platform', 't-search'),
    ('u-author', 'd-platform', 't-search'),
    ('u-reader', 'd-pay', 't-pay')
ON CONFLICT DO NOTHING;

INSERT INTO tag (id, name, active) VALUES
    ('java', 'Java', TRUE),
    ('spring-cloud', 'Spring Cloud', TRUE),
    ('redis', 'Redis', TRUE),
    ('postgresql', 'PostgreSQL', TRUE),
    ('elasticsearch', 'Elasticsearch', TRUE)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO category (id, name, active) VALUES
    ('architecture', '架构设计', TRUE),
    ('engineering', '工程实践', TRUE),
    ('database', '数据库', TRUE),
    ('operations', '运维与稳定性', TRUE)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO article (
    id,
    author_id,
    title,
    status,
    category_id,
    visibility_type,
    review_request_id,
    approved_by_review_ticket_id,
    rejected_by_review_ticket_id
) VALUES
    (
        'a-demo-draft',
        'u-author',
        'PostgreSQL 文章持久化草稿',
        'DRAFT',
        'database',
        NULL,
        NULL,
        NULL,
        NULL
    ),
    (
        'a-demo-company-published',
        'u-author',
        'Spring Cloud 内部博客实践',
        'PUBLISHED',
        'engineering',
        'COMPANY',
        NULL,
        NULL,
        NULL
    ),
    (
        'a-demo-postgresql-published',
        'u-author',
        'PostgreSQL 索引与慢查询排查',
        'PUBLISHED',
        'database',
        'COMPANY',
        NULL,
        NULL,
        NULL
    ),
    (
        'a-demo-team-review',
        'u-author',
        '搜索团队索引优化方案',
        'PENDING_REVIEW',
        'architecture',
        'TEAM',
        'rr-demo-team-search',
        NULL,
        NULL
    )
ON CONFLICT (id) DO UPDATE
SET author_id = EXCLUDED.author_id,
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    category_id = EXCLUDED.category_id,
    visibility_type = EXCLUDED.visibility_type,
    review_request_id = EXCLUDED.review_request_id,
    approved_by_review_ticket_id = EXCLUDED.approved_by_review_ticket_id,
    rejected_by_review_ticket_id = EXCLUDED.rejected_by_review_ticket_id,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO article_content (article_id, content_json, rendered_html, plain_text) VALUES
    (
        'a-demo-draft',
        $json${"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"这是一篇用于验证 PostgreSQL 持久化的草稿。"}]}]}$json$,
        '<p>这是一篇用于验证 PostgreSQL 持久化的草稿。</p>',
        '这是一篇用于验证 PostgreSQL 持久化的草稿。'
    ),
    (
        'a-demo-company-published',
        $json${"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"介绍 Spring Cloud 在企业内部博客中的服务治理实践。"}]}]}$json$,
        '<p>介绍 Spring Cloud 在企业内部博客中的服务治理实践。</p>',
        '介绍 Spring Cloud 在企业内部博客中的服务治理实践。'
    ),
    (
        'a-demo-postgresql-published',
        $json${"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"从执行计划、索引选择和统计信息三个角度排查 PostgreSQL 慢查询。"}]}]}$json$,
        '<p>从执行计划、索引选择和统计信息三个角度排查 PostgreSQL 慢查询。</p>',
        '从执行计划、索引选择和统计信息三个角度排查 PostgreSQL 慢查询。'
    ),
    (
        'a-demo-team-review',
        $json${"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"仅搜索团队可见、等待审核的索引优化方案。"}]}]}$json$,
        '<p>仅搜索团队可见、等待审核的索引优化方案。</p>',
        '仅搜索团队可见、等待审核的索引优化方案。'
    )
ON CONFLICT (article_id) DO UPDATE
SET content_json = EXCLUDED.content_json,
    rendered_html = EXCLUDED.rendered_html,
    plain_text = EXCLUDED.plain_text;

INSERT INTO article_content_version (
    article_id,
    version_no,
    title,
    content_json,
    rendered_html,
    plain_text,
    tag_ids,
    category_id,
    created_by
) VALUES
    (
        'a-demo-draft',
        1,
        'PostgreSQL 文章持久化草稿',
        $json${"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"这是一篇用于验证 PostgreSQL 持久化的草稿。"}]}]}$json$,
        '<p>这是一篇用于验证 PostgreSQL 持久化的草稿。</p>',
        '这是一篇用于验证 PostgreSQL 持久化的草稿。',
        'postgresql',
        'database',
        'u-author'
    ),
    (
        'a-demo-company-published',
        1,
        'Spring Cloud 内部博客实践',
        $json${"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"介绍 Spring Cloud 在企业内部博客中的服务治理实践。"}]}]}$json$,
        '<p>介绍 Spring Cloud 在企业内部博客中的服务治理实践。</p>',
        '介绍 Spring Cloud 在企业内部博客中的服务治理实践。',
        'java,spring-cloud',
        'engineering',
        'u-author'
    ),
    (
        'a-demo-postgresql-published',
        1,
        'PostgreSQL 索引与慢查询排查',
        $json${"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"从执行计划、索引选择和统计信息三个角度排查 PostgreSQL 慢查询。"}]}]}$json$,
        '<p>从执行计划、索引选择和统计信息三个角度排查 PostgreSQL 慢查询。</p>',
        '从执行计划、索引选择和统计信息三个角度排查 PostgreSQL 慢查询。',
        'postgresql',
        'database',
        'u-author'
    ),
    (
        'a-demo-team-review',
        1,
        '搜索团队索引优化方案',
        $json${"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"仅搜索团队可见、等待审核的索引优化方案。"}]}]}$json$,
        '<p>仅搜索团队可见、等待审核的索引优化方案。</p>',
        '仅搜索团队可见、等待审核的索引优化方案。',
        'elasticsearch',
        'architecture',
        'u-author'
    )
ON CONFLICT (article_id, version_no) DO UPDATE
SET title = EXCLUDED.title,
    content_json = EXCLUDED.content_json,
    rendered_html = EXCLUDED.rendered_html,
    plain_text = EXCLUDED.plain_text,
    tag_ids = EXCLUDED.tag_ids,
    category_id = EXCLUDED.category_id,
    created_by = EXCLUDED.created_by;

INSERT INTO blog_comment (
    id,
    article_id,
    parent_id,
    author_id,
    content,
    status
) VALUES
    (
        'c-demo-company-root',
        'a-demo-company-published',
        NULL,
        'u-reader',
        '这篇实践总结很有帮助，期待后续补充配置中心的细节。',
        'ACTIVE'
    ),
    (
        'c-demo-company-reply',
        'a-demo-company-published',
        'c-demo-company-root',
        'u-author',
        '收到，下一版会补充配置中心和灰度发布示例。',
        'ACTIVE'
    ),
    (
        'c-demo-company-hidden',
        'a-demo-company-published',
        NULL,
        'u-reader',
        '这是一条用于验证治理隐藏和恢复流程的演示评论。',
        'HIDDEN'
    )
ON CONFLICT (id) DO UPDATE
SET article_id = EXCLUDED.article_id,
    parent_id = EXCLUDED.parent_id,
    author_id = EXCLUDED.author_id,
    content = EXCLUDED.content,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

DELETE FROM article_visibility_target
WHERE article_id IN (
    'a-demo-draft',
    'a-demo-company-published',
    'a-demo-postgresql-published',
    'a-demo-team-review'
);

INSERT INTO article_visibility_target (article_id, visibility_type, target_org_id) VALUES
    ('a-demo-team-review', 'TEAM', 't-search');

DELETE FROM article_tag
WHERE article_id IN (
    'a-demo-draft',
    'a-demo-company-published',
    'a-demo-postgresql-published',
    'a-demo-team-review'
);

INSERT INTO article_tag (article_id, tag_id) VALUES
    ('a-demo-draft', 'postgresql'),
    ('a-demo-company-published', 'java'),
    ('a-demo-company-published', 'spring-cloud'),
    ('a-demo-postgresql-published', 'postgresql'),
    ('a-demo-team-review', 'elasticsearch');

INSERT INTO article_publish_record (id, article_id, published_by) VALUES
    ('pr-demo-company', 'a-demo-company-published', 'u-author'),
    ('pr-demo-postgresql', 'a-demo-postgresql-published', 'u-author')
ON CONFLICT (id) DO UPDATE
SET article_id = EXCLUDED.article_id,
    published_by = EXCLUDED.published_by;

INSERT INTO knowledge_collection (
    id,
    owner_id,
    title,
    description,
    created_at,
    updated_at
) VALUES (
    'kc-demo-backend-path',
    'u-author',
    '企业后端工程实践路径',
    '从服务治理到数据库性能排查的推荐阅读顺序。',
    CURRENT_TIMESTAMP - INTERVAL '2 days',
    CURRENT_TIMESTAMP - INTERVAL '1 hour'
)
ON CONFLICT (id) DO UPDATE
SET owner_id = EXCLUDED.owner_id,
    title = EXCLUDED.title,
    description = EXCLUDED.description,
    updated_at = EXCLUDED.updated_at;

DELETE FROM knowledge_collection_article
WHERE collection_id = 'kc-demo-backend-path';

INSERT INTO knowledge_collection_article (collection_id, article_id, position) VALUES
    ('kc-demo-backend-path', 'a-demo-company-published', 0),
    ('kc-demo-backend-path', 'a-demo-postgresql-published', 1);

INSERT INTO review_ticket (
    id,
    article_id,
    review_request_id,
    author_id,
    visibility_type,
    target_org_ids,
    status
) VALUES (
    'rt-demo-team-search',
    'a-demo-team-review',
    'rr-demo-team-search',
    'u-author',
    'TEAM',
    't-search',
    'PENDING'
)
ON CONFLICT (article_id, review_request_id) DO UPDATE
SET author_id = EXCLUDED.author_id,
    visibility_type = EXCLUDED.visibility_type,
    target_org_ids = EXCLUDED.target_org_ids,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO article_interaction (article_id, user_id, interaction_type) VALUES
    ('a-demo-company-published', 'u-admin', 'VIEW'),
    ('a-demo-company-published', 'u-author', 'VIEW'),
    ('a-demo-company-published', 'u-reader', 'VIEW'),
    ('a-demo-company-published', 'u-admin', 'LIKE'),
    ('a-demo-company-published', 'u-reader', 'LIKE'),
    ('a-demo-company-published', 'u-author', 'FAVORITE'),
    ('a-demo-company-published', 'u-reader', 'FAVORITE'),
    ('a-demo-postgresql-published', 'u-author', 'VIEW'),
    ('a-demo-postgresql-published', 'u-reader', 'VIEW'),
    ('a-demo-postgresql-published', 'u-reader', 'FAVORITE')
ON CONFLICT (article_id, user_id, interaction_type) DO NOTHING;

INSERT INTO content_subscription (
    id,
    user_id,
    target_type,
    target_id,
    created_at
) VALUES
    ('s-demo-reader-java', 'u-reader', 'TAG', 'java', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    ('s-demo-reader-engineering', 'u-reader', 'CATEGORY', 'engineering', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    ('s-demo-author-postgresql', 'u-author', 'TAG', 'postgresql', CURRENT_TIMESTAMP - INTERVAL '6 hours')
ON CONFLICT (user_id, target_type, target_id) DO UPDATE
SET created_at = EXCLUDED.created_at;

INSERT INTO user_notification (
    id,
    event_id,
    recipient_user_id,
    notification_type,
    title,
    content,
    resource_type,
    resource_id,
    read_at,
    created_at
) VALUES
    (
        'n-demo-review-approved',
        'demo-review-approved',
        'u-author',
        'REVIEW_APPROVED',
        '文章审核已通过',
        '《Spring Cloud 内部博客实践》已通过审核并发布。',
        'ARTICLE',
        'a-demo-company-published',
        NULL,
        CURRENT_TIMESTAMP - INTERVAL '20 minutes'
    ),
    (
        'n-demo-review-rejected',
        'demo-review-rejected',
        'u-author',
        'REVIEW_REJECTED',
        '文章审核未通过',
        '演示文章已退回草稿，请修改后重新提交。',
        'ARTICLE',
        'a-demo-draft',
        CURRENT_TIMESTAMP - INTERVAL '5 minutes',
        CURRENT_TIMESTAMP - INTERVAL '1 day'
    ),
    (
        'n-demo-comment-reply',
        'demo-comment-reply',
        'u-reader',
        'COMMENT_REPLY',
        '收到新的评论回复',
        'u-author 回复了你的评论：下一版会补充配置中心和灰度发布示例。',
        'ARTICLE',
        'a-demo-company-published',
        NULL,
        CURRENT_TIMESTAMP - INTERVAL '10 minutes'
    ),
    (
        'n-demo-subscription-article',
        'demo-subscription-article:u-reader',
        'u-reader',
        'SUBSCRIPTION_ARTICLE_PUBLISHED',
        '你订阅的主题有新文章',
        'Spring Cloud 内部博客实践',
        'ARTICLE',
        'a-demo-company-published',
        NULL,
        CURRENT_TIMESTAMP - INTERVAL '3 minutes'
    )
ON CONFLICT (event_id) DO UPDATE
SET title = EXCLUDED.title,
    content = EXCLUDED.content,
    resource_type = EXCLUDED.resource_type,
    resource_id = EXCLUDED.resource_id;

INSERT INTO audit_record (
    id,
    event_id,
    source_service,
    actor_id,
    actor_roles,
    action,
    resource_type,
    resource_id,
    outcome,
    details,
    trace_id,
    occurred_at
) VALUES
    (
        'demo-audit-approve',
        'demo-event-approve',
        'review-service',
        'u-reviewer',
        'REVIEWER',
        'REVIEW_APPROVE',
        'ARTICLE',
        'a-demo-team',
        'SUCCESS',
        'reviewTicket=demo-review-1',
        'demo-trace-1',
        CURRENT_TIMESTAMP - INTERVAL '3 hours'
    ),
    (
        'demo-audit-reject',
        'demo-event-reject',
        'review-service',
        'u-admin',
        'ADMIN,REVIEWER',
        'REVIEW_REJECT',
        'ARTICLE',
        'a-demo-rejected',
        'SUCCESS',
        'reviewTicket=demo-review-2; comment=内容需要补充数据来源',
        'demo-trace-2',
        CURRENT_TIMESTAMP - INTERVAL '2 hours'
    ),
    (
        'demo-audit-tag',
        'demo-event-tag',
        'tag-service',
        'u-admin',
        'ADMIN',
        'TAG_CREATE',
        'TAG',
        'observability',
        'SUCCESS',
        'name=可观测性',
        'demo-trace-3',
        CURRENT_TIMESTAMP - INTERVAL '1 hour'
    ),
    (
        'demo-audit-category',
        'demo-event-category',
        'tag-service',
        'u-admin',
        'ADMIN',
        'CATEGORY_UPDATE',
        'CATEGORY',
        'engineering',
        'SUCCESS',
        'name=工程实践',
        'demo-trace-4',
        CURRENT_TIMESTAMP - INTERVAL '30 minutes'
    ),
    (
        'demo-audit-comment-hide',
        'demo-event-comment-hide',
        'comment-service',
        'u-admin',
        'ADMIN',
        'COMMENT_HIDE',
        'COMMENT',
        'c-demo-company-hidden',
        'SUCCESS',
        'articleId=a-demo-company-published; authorId=u-reader; reason=演示治理流程',
        'demo-trace-5',
        CURRENT_TIMESTAMP - INTERVAL '15 minutes'
    )
ON CONFLICT (event_id) DO UPDATE
SET actor_id = EXCLUDED.actor_id,
    actor_roles = EXCLUDED.actor_roles,
    action = EXCLUDED.action,
    resource_type = EXCLUDED.resource_type,
    resource_id = EXCLUDED.resource_id,
    outcome = EXCLUDED.outcome,
    details = EXCLUDED.details,
    trace_id = EXCLUDED.trace_id,
    occurred_at = EXCLUDED.occurred_at;
