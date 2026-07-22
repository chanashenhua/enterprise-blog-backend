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
