# 专题与知识集合接口

专题集合由文章服务维护，所有入口都要求网关注入当前用户身份。专题只保存文章标识和顺序，读取时会重新校验每篇文章的发布状态和可见权限。

## 查询

- `GET /api/collections?mine=false&limit=20`：查询最近更新且当前用户至少可读一篇文章的专题。
- `GET /api/collections?mine=true&limit=20`：查询当前用户创建的专题。
- `GET /api/collections/{collectionId}`：查询专题详情和按顺序排列的可见文章。
- `GET /api/collections/candidates?limit=50`：查询可加入专题的已发布文章。

## 维护

- `POST /api/collections`：创建专题。
- `PUT /api/collections/{collectionId}`：由创建者或管理员更新标题、简介和文章顺序。
- `DELETE /api/collections/{collectionId}`：由创建者或管理员删除专题。

创建和更新请求示例：

```json
{
  "title": "Spring Cloud 实践路径",
  "description": "从服务发现到配置治理的推荐阅读顺序",
  "articleIds": ["article-1", "article-2"]
}
```

一个专题必须包含 2～30 篇不重复、当前用户可读的已发布文章。文章撤回、删除或权限变化后，不再出现在专题详情中。
