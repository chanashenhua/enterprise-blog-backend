# 分类与标签订阅接口

## 员工接口

接口统一经网关访问，用户身份由网关写入 `X-User-Id`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/subscriptions` | 查询当前员工的全部订阅 |
| `PUT` | `/api/subscriptions/{type}/{targetId}` | 幂等订阅，`type` 为 `TAG` 或 `CATEGORY` |
| `DELETE` | `/api/subscriptions/{type}/{targetId}` | 幂等取消订阅 |

创建订阅时，通知服务会向标签服务实时校验目标是否存在且启用。重复订阅返回原订阅记录，不会生成重复关系。

## 发布通知链路

1. 文章服务在发布全公司可见文章的同一事务中写入 `article_subscription_notification_event`；
2. 调度器将文章 ID、标题、作者、分类和标签投递给通知服务；
3. 通知服务合并所有匹配的订阅者，排除文章作者；
4. 每个接收者以 `事件 ID + 用户 ID` 作为通知幂等键，写入 `user_notification`；
5. 远程调用失败时 Outbox 保留为 `PENDING` 并重试，达到上限后标记 `FAILED`。

部门或团队文章暂不产生订阅通知，避免通知标题泄露给没有文章可见权限的订阅者。该类文章仍可通过搜索和直接访问执行实时权限校验。

## 管理端概览

`GET /api/admin/notifications/subscriptions/overview`

仅 `ADMIN` 角色可访问，返回订阅关系总数、订阅员工数、标签/分类订阅数，以及按订阅人数排序的
前 10 个主题。该接口只返回聚合结果，不暴露员工个人订阅清单。
