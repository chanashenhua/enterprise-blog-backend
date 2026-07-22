# 大型公司内部技术博客平台设计

## 状态

本文档记录 2026-07-11 与用户确认后的第一阶段设计方案。

系统面向大型公司内部编程人员，用于技术文章发布、知识沉淀、团队内共享和企业级内容治理。第一阶段目标是做一个可落地的 MVP，但架构按企业级微服务方式设计，便于后续演进。

## 目标

- 为公司内部开发者提供技术博客和知识沉淀平台。
- 接入企业级 OIDC/OAuth2 登录，不自建密码体系作为身份源。
- 支持公司、部门、团队三个层级的文章可见性。
- 支持所见即所得编辑器，重点支持代码块、表格、图片和附件。
- 使用 Elasticsearch 提供全文搜索，并保证搜索结果不泄露无权限内容。
- 使用清晰的微服务边界，但第一阶段按 MVP 节奏分阶段启用能力。
- 本地和测试环境使用 Docker Compose 一键启动，同时保留未来迁移到 Kubernetes 或企业内部平台的空间。

## MVP 不做的事情

- 不实现完整 ABAC 策略引擎。
- 不在第一阶段引入 RabbitMQ 或 Kafka。
- 不实现多人实时协同编辑。
- 不让 Elasticsearch 或 Redis 成为文章内容、权限判断的真相来源。
- 不把 MinIO 暴露成无权限控制的公共文件存储。

## 技术栈

- Nginx
- Vue 3 + Vite
- Spring Cloud Gateway
- Eureka Server
- Spring Cloud Config Server
- Spring Boot 微服务
- PostgreSQL
- Redis
- MinIO
- Elasticsearch
- Docker Compose

## 产品范围

第一阶段是企业内部技术博客 MVP，重点能力包括：

- 企业 OIDC/OAuth2 登录。
- 文章创建、草稿保存、编辑、发布、撤回和删除。
- 基于结构化内容的所见即所得编辑器。
- 标签和分类。
- 公司可见、部门可见、团队可见。
- 一篇文章可选择多个目标部门或团队。
- 部门/团队可见文章支持按策略进入审核。
- 评论和回复。
- 图片和附件上传到 MinIO。
- Elasticsearch 全文搜索。
- 员工端和管理端两个独立 Vue 应用。

## 架构方案

采用“细拆架构，分阶段启用”的方案。

服务边界按大型企业内部系统设计，但第一阶段不追求所有服务都完整平台化。核心服务先实现闭环，通知、统计、审计等服务可以轻量实现或预留。

## 入口层与基础设施

### Nginx

- 托管前端静态资源。
- 将 API 请求反向代理到 Gateway。

### gateway-service

- 统一 API 入口。
- 校验 OIDC/OAuth2 access token。
- 路由请求到下游服务。
- 做基础限流。
- 向内部服务透传可信用户上下文。

### eureka-server

- 第一阶段用于服务发现。
- 本地和测试环境通过 Docker Compose 启动。

### config-server

- 统一管理服务配置。
- 本地开发可以读取本地配置仓库。
- 未来生产环境可替换为企业配置中心或内部平台配置能力。

## 核心领域服务

### user-service

- 维护本地用户资料。
- 将企业身份 claims 映射为本地用户。
- 维护用户状态。
- 不负责账号密码体系。

### org-service

- 维护部门、团队和成员关系快照。
- 第一阶段可以用种子数据模拟组织架构。
- 后续可接入企业组织架构同步。

### permission-service

- 维护 RBAC 权限。
- 维护组织可见性判断。
- 提供类似 `check(userContext, action, resource)` 的策略判断接口。

### article-service

- 维护文章、草稿、内容版本、可见范围、发布记录和文章 outbox 事件。
- 是文章内容和文章状态的真相来源。

### comment-service

- 维护评论、回复和评论状态。

### tag-service

- 维护标签、分类和标签校验。

### review-service

- 维护审核策略、审核单、审核结果和审核历史。

### file-service

- 维护文件元数据。
- 控制 MinIO 对象访问。
- 校验上传权限、文件类型和文件大小。

### search-service

- 维护 Elasticsearch 索引。
- 提供搜索 API、高亮、排序和索引重试。
- 搜索时做权限过滤。

## 轻量实现或预留服务

### notification-service

- 预留站内通知、审核待办、订阅通知能力。
- 第一阶段可只做基础通知记录。

### stats-service

- 预留浏览、点赞、收藏和统计汇总。
- 第一阶段可先做简单计数聚合。

### audit-service

- 预留操作审计和治理查询。
- 第一阶段可先记录关键审计事件。

## 前端应用

使用两个 Vue 3 + Vite 应用。

### web-portal

面向普通开发者，包含：

- 首页信息流。
- 搜索。
- 文章详情。
- 所见即所得编辑器。
- 我的草稿。
- 我的文章。
- 收藏。
- 评论。
- 我的审核状态。

### web-admin

面向平台管理员、部门管理员、内容编辑和审核员，包含：

- 用户和组织同步状态。
- 角色和权限管理。
- 标签和分类管理。
- 审核策略管理。
- 文章治理。
- 评论治理。
- 文件治理。
- 搜索索引任务和重试。
- 审计入口。

第一阶段不单独做第三个审核工作台。作者在员工端查看提交状态，审核人在管理端处理审核任务。

## 登录与用户上下文

认证按真实企业系统优先设计。

流程：

```text
用户访问 web-portal 或 web-admin
  -> gateway-service 检查 token
  -> 未登录则跳转企业 OIDC/OAuth2 登录
  -> 登录成功后 gateway-service 校验 claims
  -> user-service 创建或更新本地用户映射
  -> org-service 提供部门和团队 membership
  -> permission-service 提供角色和权限摘要
  -> gateway-service 将 userId、roles、orgIds 透传给业务服务
```

本地开发可使用 mock OIDC 或简化登录页模拟企业 SSO，但接口形态应保持 OIDC/OAuth2 兼容。

## 权限模型

采用 RBAC + 组织可见性策略。

### RBAC 管“能做什么”

例如：

- 是否能发布文章。
- 是否能审核文章。
- 是否能管理标签。
- 是否能管理用户。
- 是否能重建搜索索引。
- 是否能查看审计记录。

### 组织可见性管“能看什么”

- 公司可见文章：所有已登录员工可读。
- 部门可见文章：目标部门成员可读。
- 团队可见文章：目标团队成员可读。

### 作者所有权管“能改什么”

- 作者可以编辑自己的草稿和符合条件的文章。
- 管理员或内容编辑可以按角色治理文章。

### 审核策略管“何时需要审批”

- 部门/团队可见文章可以根据目标部门或团队策略决定是否需要审核。
- 审核人可以来自平台角色、部门管理员或团队审核员。

第一阶段不实现完整 ABAC，但 `permission-service` 的接口应保留策略化形态，方便后续升级。

## 文章可见性

支持三类可见性：

- `company`：公司可见。
- `department`：部门可见。
- `team`：团队可见。

作者可以选择多个目标部门或团队。目标必须来自 `org-service` 的组织数据，不能由作者任意输入文本。

## 发布与审核

### 公司可见文章

默认直接发布：

```text
作者点击发布
  -> article-service 调 permission-service 校验发布权限
  -> visibility_type = company
  -> 状态变为 published
  -> 写入 ArticlePublished 事件
  -> search-service 后续索引文章
```

### 部门/团队可见文章

根据审核策略决定：

```text
作者选择目标部门或团队
  -> article-service 调 permission-service 校验目标范围
  -> review-service 判断是否需要审核
  -> 不需要审核则直接 published
  -> 需要审核则创建审核单并进入 pending_review
  -> 审核通过后 published
  -> 写入 ArticlePublished 事件
```

审核记录必须包含：

- 提交人。
- 审核人。
- 审核意见。
- 审核结果。
- 审核时间。

作者可以撤回待审核文章，修改后重新提交。

## 文章内容模型

编辑器采用所见即所得体验，建议使用 TipTap/ProseMirror 生态，不自行实现复杂编辑器。

文章内容保存三份：

- `content_json`
  - 编辑器结构化文档。
  - 是文章内容真相。
- `rendered_html`
  - 发布或保存时生成的安全 HTML。
  - 用于文章详情快速渲染。
- `plain_text`
  - 从结构化内容抽取的纯文本。
  - 用于 Elasticsearch 索引和摘要生成。

这样可以同时支持良好的编辑体验、快速展示、全文搜索和后续扩展，例如目录、内链、块级评论、协同编辑。

## PostgreSQL 数据归属

使用一个 PostgreSQL 实例，多个 database。每个服务只能直接访问自己的 database。

- `user_db`
- `org_db`
- `permission_db`
- `article_db`
- `comment_db`
- `tag_db`
- `review_db`
- `file_db`
- `search_db`
- `stats_db`
- `notification_db`
- `audit_db`

跨服务数据访问必须通过服务 API、事件或明确的快照字段完成。禁止跨 database join。

## article_db 核心表

### article

文章主表，建议字段：

- `id`
- `title`
- `summary`
- `author_id`
- `status`
- `visibility_type`
- `review_required`
- `published_at`
- `created_at`
- `updated_at`

### article_content

文章内容表，建议字段：

- `article_id`
- `version`
- `content_json`
- `rendered_html`
- `plain_text`
- `content_hash`

### article_visibility_target

文章可见范围目标表，建议字段：

- `article_id`
- `target_type`
- `target_org_id`

### article_publish_record

记录发布、撤回、重新提交等历史。

### domain_event

Outbox 事件表，建议字段：

- `event_id`
- `event_type`
- `aggregate_id`
- `payload`
- `status`
- `retry_count`
- `next_retry_at`

## 搜索索引

Elasticsearch 只保存搜索副本，不保存权限真相。

建议索引字段：

- `article_id`
- `title`
- `summary`
- `plain_text`
- `tags`
- `author_id`
- `author_name`
- `visibility_type`
- `target_org_ids`
- `status`
- `published_at`
- `updated_at`

搜索请求必须携带用户上下文：

- `user_id`
- 部门 ID 列表。
- 团队 ID 列表。
- 角色列表。

`search-service` 先在 Elasticsearch 层基于 `visibility_type` 和 `target_org_ids` 做过滤，再按需要批量调用 `permission-service` 做兜底校验。

## 核心流程

### 登录

```text
Vue 应用
  -> Nginx
  -> Gateway
  -> 企业 OIDC/OAuth2
  -> user-service / org-service / permission-service
  -> 业务服务
```

### 保存草稿

```text
web-portal 编辑器
  -> article-service
  -> 保存 article 和 article_content
  -> file-service 绑定文件引用
  -> tag-service 校验标签和分类
  -> article-service 写入 DraftSaved 事件
```

### 发布公司可见文章

```text
作者点击发布
  -> article-service 调 permission-service
  -> 判断 visibility_type = company
  -> 文章状态变为 published
  -> 写入 ArticlePublished 事件
  -> search-service 索引文章
```

### 发布部门/团队可见文章

```text
作者选择目标部门或团队
  -> article-service 调 permission-service
  -> review-service 判断审核策略
  -> 不需要审核则直接发布
  -> 需要审核则创建审核单并进入 pending_review
  -> 审核通过后 published
  -> 写入 ArticlePublished 事件
```

### 搜索

```text
用户搜索关键词
  -> search-service 查询 Elasticsearch
  -> Elasticsearch 按 visibility_type 和 target_org_ids 初筛
  -> search-service 可调用 permission-service 做最终过滤
  -> 返回带高亮的结果
```

### 文件上传

```text
用户粘贴、拖拽或上传文件
  -> file-service 校验权限、类型和大小
  -> 文件对象保存到 MinIO
  -> 文件元数据保存到 file_db
  -> 返回受控 fileId 或短期签名 URL
  -> article-service 保存文章时绑定引用关系
```

文件访问应通过短期签名 URL 或受控下载接口完成，不应开放无权限控制的 MinIO 公共地址。

## Redis 使用

Redis 第一阶段用于：

- 用户权限摘要缓存。
- 热门文章详情缓存。
- 浏览、点赞、收藏计数缓冲。

Redis 只是缓存和缓冲层，不是权限、文章内容或持久统计的真相来源。

## 事件与一致性

第一阶段不引入 RabbitMQ 或 Kafka，使用 Outbox/Event Table。

流程：

```text
业务服务在本地事务中写业务数据和 domain_event
  -> dispatcher 扫描 pending 事件
  -> 调用 search-service / notification-service / stats-service
  -> 成功后标记 delivered
  -> 失败后增加 retry_count，并按 next_retry_at 重试
```

关键事件：

- `ArticlePublished`
- `ArticleUpdated`
- `ArticleWithdrawn`
- `ArticleDeleted`
- `CommentCreated`
- `ArticleViewed`
- `ArticleLiked`

要求：

- 每个事件必须有 `event_id`。
- 消费端必须幂等。
- 搜索、通知、统计可以最终一致。
- 权限判断不能依赖最终一致，必须实时可靠。
- 搜索索引失败不能回滚已经成功的文章发布。
- 搜索结果不能泄露用户无权阅读的文章。

## 错误处理

所有服务使用统一错误响应：

```json
{
  "code": "ARTICLE_NOT_READABLE",
  "message": "You do not have permission to read this article.",
  "traceId": "trace-id",
  "details": {}
}
```

建议错误码：

- `AUTH_REQUIRED`
- `FORBIDDEN`
- `ARTICLE_NOT_READABLE`
- `REVIEW_REQUIRED`
- `VALIDATION_FAILED`
- `FILE_TYPE_NOT_ALLOWED`
- `SEARCH_INDEX_DELAYED`
- `DEPENDENCY_UNAVAILABLE`
- `OUTBOX_RETRYING`

原则：

- 权限失败必须阻断请求。
- 文章写入成功后，搜索、通知、统计失败可以走 outbox 重试。
- 依赖服务不可用时，要返回明确错误码，并带上 `traceId` 便于排查。

## 可观测性

第一阶段至少做到：

- Gateway 生成或透传 `traceId`。
- 所有服务输出 JSON 结构化日志。
- 日志包含 `traceId`、`userId`、服务名、路径、状态码、耗时。
- 所有 Spring Boot 服务暴露 Actuator 健康检查。
- Docker Compose 给 PostgreSQL、Redis、MinIO、Elasticsearch 配健康检查。
- 管理端提供搜索索引失败任务列表和重试入口。

MVP 可以暂不接入完整链路追踪平台，但日志字段应先统一，方便后续接入。

## 测试策略

测试重点放在权限、发布审核、搜索可见性、文件安全和事件重试。

### 单元测试

- 权限规则。
- 文章状态机。
- 审核策略判断。
- 结构化内容转 HTML 和纯文本。
- 文件类型和大小校验。

### 服务测试

- 每个服务 API。
- 数据库迁移。
- outbox 调度和重试。
- 事件消费者幂等。

### 集成测试

- 文章发布后进入搜索索引。
- 审核通过后文章可搜索。
- 文章撤回或删除后从搜索结果中移除。
- 文件上传后可绑定到文章内容。

### 端到端测试

- 登录。
- 写文章。
- 上传图片。
- 提交审核。
- 审核通过。
- 搜索文章。
- 评论文章。

必须覆盖的风险场景：

- 用户不能读取非目标部门/团队文章。
- 搜索结果不能泄露不可见文章。
- 审核中文章不能被无关用户读取。
- MinIO 文件不能绕过权限直接访问。
- Elasticsearch 索引失败不影响文章发布，并且可以后台重试。
- outbox 重复投递不会造成重复索引或重复通知。

## 部署方式

本地和测试环境使用 Docker Compose。

Compose 应启动：

- Nginx
- `web-portal`
- `web-admin`
- Gateway
- Eureka Server
- Config Server
- 核心微服务
- PostgreSQL
- Redis
- MinIO
- Elasticsearch

配置按环境隔离：

- `dev`
- `test`
- `prod-like`

第一阶段部署结构要保持可迁移，后续可以迁移到 Kubernetes 或公司内部 PaaS。

## 实现注意事项

- 本地开发使用 mock OIDC，但不要把本地账号密码登录写死成长期方案。
- 即使某些服务第一阶段功能较轻，也要保留清晰服务边界。
- 禁止跨 database join。
- 事件 payload 从一开始就要带版本号。
- Elasticsearch mapping 应显式定义，避免完全依赖动态 mapping。
- 准备用户、组织、角色、文章等种子数据，让本地环境启动后可直接演示。

## 推荐结论

推荐按“细拆架构，分阶段启用”推进。

这套方案的核心价值是：

- 有企业级架构边界。
- 不让第一阶段被完整平台化拖垮。
- 文章服务和权限服务保持真相来源。
- 搜索强大但不承担最终权限判断。
- 内容格式适合长期演进。
- PostgreSQL 数据归属清晰。
- Redis、Elasticsearch、MinIO 各司其职。
- Outbox 提供事件驱动结构，同时避免第一阶段引入 MQ 的复杂度。
