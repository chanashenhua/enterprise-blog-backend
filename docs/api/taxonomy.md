# 标签与分类 API

标签和分类由 `tag-service` 统一持久化管理。删除操作实际是停用：历史文章继续保留原标识，
但新建或编辑文章时不能再选择已停用项。

## 员工端目录

```http
GET /api/tags
GET /api/categories
```

只返回 `active=true` 的条目，响应字段为 `id`、`name`、`active`。

文章草稿的创建和编辑请求支持：

```json
{
  "title": "文章标题",
  "contentJson": {},
  "tagIds": ["java", "postgresql"],
  "categoryId": "engineering"
}
```

`categoryId` 可为空；非空时必须是启用中的分类。标签和分类会进入文章内容版本快照，并随
发布事件写入搜索索引。搜索接口支持 `categoryId` 与 `tagId` 查询参数。

## 管理接口

以下接口要求网关注入 `ADMIN` 角色：

```http
GET    /api/admin/tags
POST   /api/admin/tags
PUT    /api/admin/tags/{id}
DELETE /api/admin/tags/{id}

GET    /api/admin/categories
POST   /api/admin/categories
PUT    /api/admin/categories/{id}
DELETE /api/admin/categories/{id}
```

创建请求：

```json
{
  "id": "security",
  "name": "安全实践"
}
```

`id` 只允许小写字母、数字和连字符，最长 64 个字符；`name` 最长 80 个字符。`PUT`
会更新名称并重新启用条目，`DELETE` 成功返回 `204`。
