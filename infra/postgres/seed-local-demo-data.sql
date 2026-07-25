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

INSERT INTO tag (id, name) VALUES
    ('java', 'Java'),
    ('spring-cloud', 'Spring Cloud'),
    ('redis', 'Redis'),
    ('postgresql', 'PostgreSQL'),
    ('elasticsearch', 'Elasticsearch')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO article (
    id,
    author_id,
    title,
    status,
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
        'TEAM',
        'rr-demo-team-search',
        NULL,
        NULL
    )
ON CONFLICT (id) DO UPDATE
SET author_id = EXCLUDED.author_id,
    title = EXCLUDED.title,
    status = EXCLUDED.status,
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
        'u-author'
    )
ON CONFLICT (article_id, version_no) DO UPDATE
SET title = EXCLUDED.title,
    content_json = EXCLUDED.content_json,
    rendered_html = EXCLUDED.rendered_html,
    plain_text = EXCLUDED.plain_text,
    tag_ids = EXCLUDED.tag_ids,
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
    )
ON CONFLICT (id) DO UPDATE
SET article_id = EXCLUDED.article_id,
    parent_id = EXCLUDED.parent_id,
    author_id = EXCLUDED.author_id,
    content = EXCLUDED.content,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

DELETE FROM article_visibility_target
WHERE article_id IN ('a-demo-draft', 'a-demo-company-published', 'a-demo-team-review');

INSERT INTO article_visibility_target (article_id, visibility_type, target_org_id) VALUES
    ('a-demo-team-review', 'TEAM', 't-search');

DELETE FROM article_tag
WHERE article_id IN ('a-demo-draft', 'a-demo-company-published', 'a-demo-team-review');

INSERT INTO article_tag (article_id, tag_id) VALUES
    ('a-demo-draft', 'postgresql'),
    ('a-demo-company-published', 'java'),
    ('a-demo-company-published', 'spring-cloud'),
    ('a-demo-team-review', 'elasticsearch');

INSERT INTO article_publish_record (id, article_id, published_by) VALUES
    ('pr-demo-company', 'a-demo-company-published', 'u-author')
ON CONFLICT (id) DO UPDATE
SET article_id = EXCLUDED.article_id,
    published_by = EXCLUDED.published_by;

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
