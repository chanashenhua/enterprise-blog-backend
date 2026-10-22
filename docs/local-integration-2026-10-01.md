# 2026-10-01 本机真实联调记录

本记录补充此前完成但尚未提交的真实环境验收。环境为 Java 17、本机 PostgreSQL、Gateway 与 12 个本地后台服务，以及两个 Vite 前端；当时没有启动 Elasticsearch，搜索 Outbox 暂停并保留待发送事件。

- V8 迁移前创建了 PostgreSQL 自定义格式备份：`target/local-integration/2026-10-01/postgres-before-article-v8.dump`。本机 `local` Profile 关闭 Flyway，因此 V8 以单事务手工应用一次。
- 原有 4 篇演示文章保留，修订初始化为 1。没有重新运行演示种子脚本。
- 后端全量测试 193 项通过；文章服务 73 项。
- 新增独立 `playwright.live.config.ts`，7 项真实 Gateway/PostgreSQL/浏览器测试通过：并发创建幂等、修订冲突、保存发布竞态、自动保存与历史、离线恢复、双窗口冲突、审核通过与退回。
- 测试产生的 8 篇 `LIVE-` 文章已软删除，关联测试审核单已结束。历史、通知、审计按业务规则保留。
- 本地服务启停脚本记录 PID、启动时间和 JAR 路径，启动逐个检查健康，停止验证实际进程；日志与数据库核验保存在 `target/local-integration/2026-10-01`。

以上记录仅对应当次环境。Elasticsearch 的后续安装、索引一致性与搜索 UI 见 [本地搜索指南](local-elasticsearch.md) 和 [搜索接口](api/search.md)，不能用当时暂停索引的测试代替搜索验收。
