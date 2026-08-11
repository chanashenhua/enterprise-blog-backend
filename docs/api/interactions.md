# 文章互动 API

互动接口统一位于 `/api/articles/{articleId}/interactions`。所有接口都要求 Gateway 提供
`X-User-Id` 等身份头，并在操作前调用文章服务确认当前用户可以读取文章。

## 获取互动摘要

```http
GET /api/articles/{articleId}/interactions
```

返回文章的去重浏览数、点赞数、收藏数，以及当前用户是否已点赞、已收藏。

## 记录浏览

```http
POST /api/articles/{articleId}/interactions/views
```

同一用户对同一文章只累计一次浏览。重复请求仍返回 `200` 和最新摘要，不会重复计数。

## 点赞与取消点赞

```http
PUT /api/articles/{articleId}/interactions/likes
DELETE /api/articles/{articleId}/interactions/likes
```

两个接口均为幂等操作，并返回操作后的最新摘要。

## 收藏与取消收藏

```http
PUT /api/articles/{articleId}/interactions/favorites
DELETE /api/articles/{articleId}/interactions/favorites
```

两个接口均为幂等操作，并返回操作后的最新摘要。收藏当前只提供用户状态与汇总数量，
后续可以在员工端增加“我的收藏”列表。

## 数据一致性

`stats-service` 使用 PostgreSQL 的 `(article_id, user_id, interaction_type)` 联合主键保存互动事实。
数据库是持久化真相来源；未来使用 Redis 缓冲热门计数时，仍应以该表校准数据。
