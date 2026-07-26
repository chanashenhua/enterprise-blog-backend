# 本地演示环境

## 准备

```powershell
Copy-Item .env.example .env
# 在 .env 中填写所有密钥；开发环境可使用不包含生产凭据的随机字符串。
docker compose config
docker compose up -d
docker compose ps
```

## 服务地址

- gateway-service: http://localhost:8080
- eureka-server: http://localhost:8761
- config-server: http://localhost:8888
- minio console: http://localhost:9001
- elasticsearch: http://localhost:9200

## 种子数据

用户、组织与标签由各服务的 Flyway migration 初始化。若需要在已运行环境中重复恢复演示身份，运行：

```powershell
Get-Content infra/postgres/seed-demo-data.sql | docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U blog -d postgres
```

### 不使用 Docker 的本机 PostgreSQL

本机单库调试使用 `jdbc:postgresql://localhost:5432/postgres`。密码只通过当前终端环境变量传入，
不要写入 Git。文章服务的 `V3`～`V5`、标签服务的 `V3`、评论服务的 `V1`～`V2`、
统计服务与通知服务的 `V1` 迁移只在数据库
尚无对应结构时各执行一次；
种子脚本可重复执行：

```powershell
$env:PGPASSWORD = Read-Host 'PostgreSQL password'
psql -v ON_ERROR_STOP=1 -h localhost -p 5432 -U postgres -d postgres `
  -f article-service/src/main/resources/db/migration/V3__persist_article_aggregate.sql
psql -v ON_ERROR_STOP=1 -h localhost -p 5432 -U postgres -d postgres `
  -f article-service/src/main/resources/db/migration/V4__article_content_versions.sql
psql -v ON_ERROR_STOP=1 -h localhost -p 5432 -U postgres -d postgres `
  -f tag-service/src/main/resources/db/migration/V3__managed_taxonomy.sql
psql -v ON_ERROR_STOP=1 -h localhost -p 5432 -U postgres -d postgres `
  -f article-service/src/main/resources/db/migration/V5__article_category.sql
psql -v ON_ERROR_STOP=1 -h localhost -p 5432 -U postgres -d postgres `
  -f comment-service/src/main/resources/db/migration/V1__comments.sql
psql -v ON_ERROR_STOP=1 -h localhost -p 5432 -U postgres -d postgres `
  -f comment-service/src/main/resources/db/migration/V2__comment_notification_outbox.sql
psql -v ON_ERROR_STOP=1 -h localhost -p 5432 -U postgres -d postgres `
  -f stats-service/src/main/resources/db/migration/V1__article_interactions.sql
psql -v ON_ERROR_STOP=1 -h localhost -p 5432 -U postgres -d postgres `
  -f notification-service/src/main/resources/db/migration/V1__notifications.sql
psql -v ON_ERROR_STOP=1 -h localhost -p 5432 -U postgres -d postgres `
  -f infra/postgres/seed-local-demo-data.sql
Remove-Item Env:PGPASSWORD
```

启动文章服务时使用 `local` Profile，并通过 `POSTGRES_PASSWORD` 提供密码。
`config-repo/article-service-local.yml` 会连接本机单库并关闭 Flyway，因为多个服务共用
`public` schema 时不能共用一张 `flyway_schema_history`。

本机种子脚本会建立三篇文章：

- PostgreSQL 持久化草稿；
- 全公司可见的已发布文章；
- 搜索团队可见的待审核文章及对应审核单。

每篇演示文章还会建立一个初始内容版本，可用于验证文章版本列表接口。
已发布文章会建立一条评论和一条作者回复，可用于验证评论线程接口。
启动 `stats-service` 后，访问已发布文章会建立去重浏览记录，并可验证点赞与收藏接口。
通知服务的演示数据包含审核通过、审核退回和评论回复通知，可用于验证未读数与已读状态。

## 核心验收

先在相邻的 `enterprise-blog-frontend` 仓库启动员工端与管理端，再执行以下流程。

1. 使用 `u-author` 创建公司可见文章。
2. 在搜索页搜索标题，能看到该文章。
3. 使用 `u-author` 创建 `t-search` 团队可见文章。
4. 文章进入待审核。
5. 使用 `u-admin` 在管理端审核通过。
6. 使用 `u-reader` 搜索相同标题，看不到该团队文章。
7. 使用 `u-author` 搜索相同标题，可以看到该团队文章。

## 端到端测试

在 Compose 服务全部启动、前端应用也启动后，在 `enterprise-blog-frontend` 仓库运行：

```powershell
cd e2e
pnpm install --frozen-lockfile
pnpm exec playwright install chromium
pnpm test
```

默认地址为员工端 `http://localhost:5173` 与管理端 `http://localhost:5174/admin/`。如需覆盖，设置 `E2E_PORTAL_URL` 和 `E2E_ADMIN_URL`。
