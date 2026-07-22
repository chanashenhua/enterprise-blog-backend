COMMENT ON TABLE article IS '文章基础信息与发布状态';
COMMENT ON COLUMN article.id IS '文章唯一标识';
COMMENT ON COLUMN article.author_id IS '作者用户标识';
COMMENT ON COLUMN article.title IS '文章标题';
COMMENT ON COLUMN article.status IS '文章状态，如 DRAFT、PENDING_REVIEW、PUBLISHED';
COMMENT ON COLUMN article.created_at IS '文章创建时间';
COMMENT ON COLUMN article.updated_at IS '文章最后更新时间';

COMMENT ON TABLE article_content IS '文章编辑器内容及其展示、检索投影';
COMMENT ON COLUMN article_content.article_id IS '关联的文章标识';
COMMENT ON COLUMN article_content.content_json IS '编辑器保存的原始 JSON 内容';
COMMENT ON COLUMN article_content.rendered_html IS '经过受控渲染和转义的展示 HTML';
COMMENT ON COLUMN article_content.plain_text IS '用于摘要和全文检索的纯文本内容';

COMMENT ON TABLE article_visibility_target IS '文章按部门或团队限定可见范围的目标组织';
COMMENT ON COLUMN article_visibility_target.article_id IS '关联的文章标识';
COMMENT ON COLUMN article_visibility_target.visibility_type IS '可见范围类型，如 DEPARTMENT 或 TEAM';
COMMENT ON COLUMN article_visibility_target.target_org_id IS '允许阅读的目标组织标识';

COMMENT ON TABLE article_publish_record IS '文章发布操作记录';
COMMENT ON COLUMN article_publish_record.id IS '发布记录唯一标识';
COMMENT ON COLUMN article_publish_record.article_id IS '被发布的文章标识';
COMMENT ON COLUMN article_publish_record.published_by IS '执行发布操作的用户标识';
COMMENT ON COLUMN article_publish_record.published_at IS '文章发布时间';

COMMENT ON TABLE domain_event IS 'Outbox 模式的待投递领域事件';
COMMENT ON COLUMN domain_event.id IS '领域事件唯一标识';
COMMENT ON COLUMN domain_event.aggregate_type IS '产生事件的聚合类型';
COMMENT ON COLUMN domain_event.aggregate_id IS '产生事件的聚合标识';
COMMENT ON COLUMN domain_event.event_type IS '事件类型，如 ArticlePublished';
COMMENT ON COLUMN domain_event.payload_json IS '事件投递所需的 JSON 快照';
COMMENT ON COLUMN domain_event.status IS '投递状态，如 PENDING、DELIVERED、FAILED';
COMMENT ON COLUMN domain_event.retry_count IS '事件投递失败后的累计重试次数';
COMMENT ON COLUMN domain_event.created_at IS '事件写入 Outbox 的时间';
COMMENT ON COLUMN domain_event.updated_at IS '事件状态最后更新时间';
