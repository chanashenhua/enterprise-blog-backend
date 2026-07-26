# 评论与回复 API

评论接口统一位于文章资源下，并要求网关提供 `X-User-Id`。服务会先调用文章服务验证当前用户
能够读取该文章，避免通过评论接口泄露不可见文章的信息。

## 创建评论

```http
POST /api/articles/{articleId}/comments
Content-Type: application/json

{
  "content": "这篇文章很有帮助"
}
```

创建一层回复时传入根评论标识：

```json
{
  "content": "补充一个实践案例",
  "parentId": "comment-id"
}
```

回复不能继续嵌套。正文去除首尾空白后不能为空，最长 2000 个字符。

## 查询评论线程

```http
GET /api/articles/{articleId}/comments
```

结果按创建时间正序返回。已删除评论仍保留位置与回复关系，`deleted` 为 `true`，`content`
为 `null`。

## 编辑评论

```http
PUT /api/articles/{articleId}/comments/{commentId}
Content-Type: application/json

{
  "content": "更新后的内容"
}
```

只有评论作者或 `ADMIN` 可以编辑，已删除评论不能编辑。

## 删除评论

```http
DELETE /api/articles/{articleId}/comments/{commentId}
```

只有评论作者或 `ADMIN` 可以删除。删除为幂等软删除，不会级联删除回复，成功返回 `204`。
