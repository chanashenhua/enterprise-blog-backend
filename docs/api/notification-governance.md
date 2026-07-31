# 通知治理接口

管理端通知治理用于观察站内通知的覆盖和未读情况，不允许管理员代替员工修改已读状态。

## 汇总

```http
GET /api/admin/notifications/overview
```

返回通知总数、已读数、未读数、接收人数，以及按通知类型汇总的总数和未读数。

## 查询

```http
GET /api/admin/notifications?recipientUserId=u-author&type=COMMENT_REPLY&state=UNREAD&limit=100
```

可选条件：

- `recipientUserId`：接收员工 ID；
- `type`：通知类型；
- `state`：`ALL`、`READ` 或 `UNREAD`；
- `limit`：返回数量，范围 1～200。

两个接口均要求 `ADMIN` 角色。管理端只查询通知状态；员工仍通过 `/api/notifications/**` 管理自己的已读状态。
