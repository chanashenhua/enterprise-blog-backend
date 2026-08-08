# 评论治理接口

所有接口经网关访问，仅 `ADMIN` 角色可调用。

## 全局概览

`GET /api/admin/comments/overview`

返回评论总数、正常/隐藏/删除数量、涉及文章数和评论作者数。该统计不受评论列表筛选条件影响。

## 查询评论

`GET /api/admin/comments`

可选参数：

| 参数 | 说明 |
| --- | --- |
| `articleId` | 精确筛选文章 |
| `authorId` | 精确筛选评论作者 |
| `status` | `ALL`、`ACTIVE`、`HIDDEN` 或 `DELETED` |
| `limit` | 返回上限，范围 1～200，默认 100 |

管理端响应保留治理所需的原评论正文；员工端读取评论时，`HIDDEN` 和 `DELETED` 状态均不返回正文。

## 隐藏与恢复

- `POST /api/admin/comments/{commentId}/hide`
- `POST /api/admin/comments/{commentId}/restore`

请求体：

```json
{ "reason": "违反社区交流规范" }
```

隐藏不会删除正文，复核后可以恢复。用户已经软删除的评论不能隐藏或恢复。重复隐藏和重复恢复按幂等成功处理，不重复生成审计事件。

## 审计可靠性

评论状态变更与 `comment_audit_event` 在同一个 PostgreSQL 事务中写入。调度器将 `COMMENT_HIDE` 或
`COMMENT_RESTORE` 事件投递给审计服务，失败时保留并重试，确保治理动作最终可追溯。
