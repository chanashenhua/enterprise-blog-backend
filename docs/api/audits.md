# 关键操作审计接口

## 管理端查询

审计查询统一由 Gateway 暴露为：

```http
GET /api/admin/audits
```

调用者必须具有 `ADMIN` 角色。支持以下可选查询参数：

- `actorId`：操作人 ID，精确匹配；
- `action`：标准化动作，例如 `REVIEW_APPROVE`、`TAG_CREATE`；
- `resourceType`：资源类型，例如 `ARTICLE`、`TAG`、`CATEGORY`；
- `resourceId`：资源 ID，精确匹配；
- `from`、`to`：ISO-8601 时间范围；
- `limit`：返回数量，范围 1～200，默认 100。

结果按业务发生时间倒序返回，包含来源服务、操作人及角色、动作、资源、结果、详情、Trace ID 和时间。

## 内部写入

审核服务和标签服务通过本地 Outbox 异步投递：

```http
POST /internal/audits
X-Internal-Token: <INTERNAL_AUDIT_TOKEN>
Content-Type: application/json
```

`eventId` 具有唯一约束。上游重复投递同一事件时，审计服务返回已有记录，不会生成重复审计数据。

当前记录的关键动作包括：

- 审核通过、审核驳回；
- 分类新增、修改、停用；
- 标签新增、修改、停用。
