# Windows 本地 Elasticsearch

无需服务器或 Docker。此项目本地调试使用 Elasticsearch **8.15.3**，与仓库现有 Compose 版本一致；它不是最新版。安装来源为 [Elastic 官方历史版本](https://www.elastic.co/downloads/past-releases/elasticsearch-8-15-3)，下载的是 Windows x86_64 ZIP，已核验官方 SHA-512。

## 本机目录

| 内容 | 路径 |
| --- | --- |
| 安装包及 SHA-512 | `D:\ElasticsearchDownloads\elasticsearch-8.15.3-windows-x86_64.zip`、同名 `.sha512` |
| 解压安装 | `D:\Elasticsearch\elasticsearch-8.15.3` |
| 数据、日志、配置、临时文件 | `D:\Elasticsearch\local-blog` 下的 `data`、`logs`、`config`、`temp` |
| Maven 依赖缓存 | `D:\Maven\enterprise-blog-repository` |
| npm 下载缓存 | 在安装前设置 `$env:npm_config_cache='D:\NodeCache\enterprise-blog'` |

Elasticsearch 使用安装包自带 JDK，与业务服务使用的 Java 17 相互独立。不要删除 `data` 目录来重启服务。

## 启动

在后端仓库打开 PowerShell 7.4 或以上：

```powershell
.\scripts\start-local-elasticsearch.ps1
Invoke-RestMethod http://127.0.0.1:9200/_cluster/health -NoProxy
```

脚本绑定 `127.0.0.1:9200`，单节点、512 MB 堆，后台启动，数据与临时文件全部放在 D 盘。可用 `-HeapMb 1024` 调整堆大小；默认安装和运行目录也可通过参数改为 D 盘其他目录。首次创建专用配置，以后保留 JVM 与日志配置。此本地实例关闭 ES 身份认证，仅供本机调试。

然后构建和启动业务服务。已完成本机数据库迁移时不需要重新初始化数据库，也不要重复运行种子脚本：

```powershell
$env:JAVA_HOME='D:\Java\jdk-17'
$maven='D:\Maven\apache-maven-3.8.8\bin\mvn.cmd'
& $maven '-Dmaven.repo.local=D:\Maven\enterprise-blog-repository' install -DskipTests
& $maven '-Dmaven.repo.local=D:\Maven\enterprise-blog-repository' -pl eureka-server,config-server,gateway-service,org-service,permission-service,article-service,tag-service,review-service,comment-service,stats-service,notification-service,audit-service,search-service package spring-boot:repackage -DskipTests
$env:POSTGRES_PASSWORD=Read-Host '本机 PostgreSQL 密码' -MaskInput
.\scripts\start-local-services.ps1 -IncludeSearch
Remove-Item Env:POSTGRES_PASSWORD
```

`-IncludeSearch` 增加 8088 搜索服务，并为文章服务启用搜索 Outbox。脚本在端口已占用时拒绝重复启动；如果已有不含搜索的本地服务，先停止它们再按上述方式启动，或单独启动搜索并带 `-IncludeSearch -ServiceNames article-service` 重启文章服务。仅启动搜索服务不会改变已运行文章服务的投递开关。

使用 IDEA 时，搜索服务启用 `local` Profile；文章服务另外配置 `BLOG_OUTBOX_SEARCH_ENABLED=true`。默认不带搜索的 `local` 配置暂停投递并保留事件，方便不使用 Elasticsearch 时继续编辑。两个服务自定义内部令牌时必须保持 `INTERNAL_SEARCH_TOKEN` 相同。

## 使用

1. 员工端 `http://127.0.0.1:5173/search`，登录后输入关键词、选择分类／标签和排序。
2. 管理端 `http://127.0.0.1:5174/search-tasks`，使用 `u-admin` 登录，首次运行点击“重新同步全部文章”，将现有数据库文章加入索引。
3. 发布文章后等待数秒；团队／部门文章通过审核后才可搜索。撤回或删除后结果应立即被读取校验过滤。
4. 如果搜索服务曾离线，恢复后在管理端查看失败任务并重试；待处理任务由后台继续投递。

健康检查：`http://127.0.0.1:8088/actuator/health` 同时检查 ES 集群和文章索引健康。接口、修订保护和分页边界见 [搜索接口](api/search.md)。

## 停止与日志

```powershell
.\scripts\stop-local-services.ps1
.\scripts\stop-local-elasticsearch.ps1 -WhatIf
.\scripts\stop-local-elasticsearch.ps1
```

两个停止脚本均核对 PID、启动时间与命令行，避免停止无关进程。业务日志默认在 `target/local-services`；若启动时指定 `-LogDirectory`，停止时传入同一目录。ES 控制台日志在 `D:\Elasticsearch\local-blog\elasticsearch.out.log` / `.err.log`，集群日志在其 `logs` 子目录。

搜索端到端测试在前端仓库运行 `npm run test:live:search`（进入 `e2e`）。测试使用已安装的 Chrome，只创建和清理有唯一前缀的测试文章；会对这些文章注入旧索引，验证当前权限和修订校验，并通过全量同步接口修复索引。

单独执行停机恢复验证：`.\scripts\test-local-search-recovery.ps1`。该脚本会停止并重新启动受管理的本地 ES，检查离线搜索 503、发布／撤回／删除的 5 次失败上限，以及管理重试后的当前索引。仅在本地调试栈运行，不与其他联调测试并行；结束后恢复 ES 并软删除本次唯一前缀文章。
