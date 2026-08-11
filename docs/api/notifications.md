# 站内通知 API

员工接口统一位于 `/api/notifications`，只读取 Gateway 注入的当前用户身份，不接受客户端指定接收人。

## 通知列表与未读数

```http
GET /api/notifications?limit=50
GET /api/notifications/unread-count
```

列表按创建时间倒序返回，默认最多 50 条，服务端上限为 100 条。未读数响应格式为：

```json
{"count": 2}
```

## 标记已读

```http
PUT /api/notifications/{notificationId}/read
PUT /api/notifications/read-all
```

单条已读操作只能修改当前用户自己的通知；重复调用保持幂等。全部已读返回操作后的未读数。

## 内部创建接口

```http
POST /internal/notifications
X-Internal-Token: <service token>
```

文章和评论 Outbox 调度器通过该接口创建通知。请求中的 `eventId` 具有唯一约束，同一事件至少一次投递
也只会生成一条通知。当前接入事件：

- `REVIEW_APPROVED`：文章审核通过并发布；
- `REVIEW_REJECTED`：文章审核退回草稿；
- `COMMENT_REPLY`：其他员工回复了当前用户的评论。
