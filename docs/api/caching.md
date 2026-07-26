# 目录缓存

标签与分类是读多写少的目录数据，`tag-service` 对以下结果启用 Spring Cache：

- 员工端和管理端目录列表；
- 标签批量校验；
- 分类有效性校验。

常规服务配置使用 Redis，缓存 TTL 默认为 5 分钟：

```text
spring.cache.type=redis
spring.data.redis.host=redis
blog.cache.catalog-ttl=PT5M
```

管理员创建、改名、重新启用或停用任意标签/分类后，会立即清空所有目录与校验缓存。因此写入
成功后不需要等待 TTL 才能看到新状态。

本机无 Docker 调试使用 `config-repo/tag-service-local.yml`，缓存类型为 `simple`，只使用当前
Java 进程内存，不连接 Redis。进程重启后缓存自然清空；这个模式仅用于开发和测试，不适合作为
多实例生产缓存。

缓存内容不包含用户权限结果、文章正文或 Bearer 令牌，避免身份变更或文章撤回后出现越权读取。
