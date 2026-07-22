# 错误码

所有 API 错误响应均包含 `code`、`message`、`traceId` 和 `details`。客户端应基于 `code` 做稳定处理，不依赖 `message` 文本。

| code | HTTP 状态 | 含义 |
| --- | --- | --- |
| `ARTICLE_NOT_READABLE` | 403 | 当前用户不具备文章可见性范围权限。 |
| `ARTICLE_NOT_EDITABLE` | 403 | 当前用户不是文章所有者且不是管理员。 |
| `ARTICLE_NOT_PUBLISHABLE` | 409 | 文章当前状态不能提交发布。 |
| `USER_OUTSIDE_TARGET_ORG` | 403 | 用户不在目标部门或团队中。 |
| `REVIEW_NOT_ALLOWED` | 403 | 当前用户没有审核权限。 |
| `FILE_TYPE_NOT_ALLOWED` | 400 | 上传文件类型不在允许列表。 |
| `FILE_SIZE_EXCEEDED` | 400 | 上传文件超过大小限制。 |
| `INTERNAL_TOKEN_INVALID` | 401 | 内部服务调用令牌无效。 |
