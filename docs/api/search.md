# Elasticsearch 搜索与索引同步

## 员工搜索

`GET /api/search/articles` 经 Gateway 认证后访问。仅返回当前用户可读取的已发布文章。

| 参数 | 说明 |
| --- | --- |
| `q` | 关键词，最多 200 字符；空字符串浏览可访问文章 |
| `categoryId`、`tagId` | 分类／标签 ID，精确过滤，可同时使用 |
| `sort` | `relevance`（默认）或 `latest`；无关键词时按发布时间倒序 |
| `page` | 从 0 开始，范围 0–99 |
| `size` | 1–50，默认 20 |

关键词在标题、摘要和全文中匹配，多个词可跨字段出现；标题和摘要提高相关性权重。排序最终以文章 ID 稳定排序。使用 Elasticsearch 内置 standard 分词，不需要额外下载插件。

响应包含 `items`、`page`、`size`、`total`、`hasMore`、`limited`。每个结果保留标题、摘要、标签、分类、作者及时间，并新增 `titleHighlights`、`summaryHighlights`：

```json
[{"text":"Redis","matched":true},{"text":" 缓存实践","matched":false}]
```

高亮是文本片段，前端使用文本插值与 `<mark>` 渲染，不执行返回内容中的 HTML。

搜索先按公司／部门／团队范围过滤 ES 候选，再批量调用文章服务，核对数据库的**当前发布状态、当前修订和当前读取权限**。未通过校验的内容不进入响应，也不计入可访问总数。权限校验或 ES 不可用时返回 503，前端允许重试。

分页在校验后进行，每批校验最多 100 个候选，每次搜索最多扫描 10,000 个候选。扫描完全部候选时，`total` 才是准确的可访问文章数；否则为 `null`，由 `hasMore` 判断是否可翻页。达到扫描上限时 `limited=true`，需缩小搜索范围。并发发布或撤回时，翻页结果可能变化，本接口不提供固定快照游标。

## 索引一致性

索引为 `articles-v1`，严格字段定义来源于 `infra/elasticsearch/article-index.json`。搜索服务启动时自动创建缺失索引，并向既有索引添加映射；不删除已有索引或数据。索引包含 `category_id` 和 `revision`。

文章服务每 5 秒处理最多 20 个待发送搜索事件。发布、撤回、删除及主动同步均重新读取并锁定文章当前记录，不使用旧事件中的正文快照。远程成功后才标记 `DELIVERED`；失败累计重试，默认第 5 次标记 `FAILED`。

ES 写入使用文章修订作为 `external_gte` 版本。旧修订写入冲突按已被新版本替代处理，同修订重试幂等。撤回、删除或非发布状态会擦除索引正文，仅保留文章 ID、修订号及 `DELETED` 标记，长期阻止迟到的旧发布请求恢复内容。重新编辑、发布产生更高修订后可以恢复检索。

索引更新存在 Outbox 和 ES 刷新延迟，通常需数秒。文章撤回或删除后，即使 ES 尚未更新，文章服务的当前状态校验也会阻止旧结果被返回。

内部接口仅供服务间调用，不通过 Gateway 暴露：

- `POST /internal/search/articles/index`：当前索引投影，必须包含正数 `revision`。
- `DELETE /internal/search/articles/{id}?revision=N`：写入无正文的修订标记。
- `POST /internal/articles/search-access`：批量 `{articleId, revision}`，返回允许读取的 ID。

三个接口均校验 `X-Internal-Token`；两服务使用相同的 `INTERNAL_SEARCH_TOKEN`。本地默认值仅供回环地址调试。

## 管理端

管理台 `/search-tasks` 提供失败任务、待处理任务、重试和重新同步全部文章。

| 接口 | 行为 |
| --- | --- |
| `GET /api/admin/search/tasks?status=FAILED` | 状态为 `FAILED` 或 `PENDING`，最多 200 项，包含 `eventType` |
| `POST /api/admin/search/tasks/{eventId}/retry` | 仅将失败的搜索任务重置为待处理并清零重试次数，成功返回 204 |
| `POST /api/admin/search/tasks/reindex` | 为所有状态的文章排入同步任务，跳过已有待处理同步任务，返回 `queued` 数量 |

需要 ADMIN 身份；普通作者、读者不能操作。任务覆盖 `ArticlePublished`、`ArticleWithdrawn`、`ArticleDeleted`、`ArticleSearchSync`，不改动审核或通知 Outbox。全量同步保留数据库正文、历史与现有索引；它会修复既有文章的索引，也会清理已撤回／删除文章的索引内容。

本地安装和启停见 [Windows 本地 Elasticsearch](../local-elasticsearch.md)。搜索模块没有新增数据库迁移，但要求文章服务已具备 V8 修订号结构，文章服务与搜索服务必须配套升级。
