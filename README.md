# 企业技术博客后端

该仓库包含 Spring Boot 微服务、Spring Cloud 配置、PostgreSQL/Redis/MinIO/Elasticsearch 基础设施与本地 Compose 编排。

## 启动

```powershell
Copy-Item .env.example .env
# 填写 .env 中的开发环境密钥
docker compose up -d
```

Gateway 地址为 `http://localhost:8080`。员工端和管理端位于相邻的 `enterprise-blog-frontend` 仓库，通过 Gateway 的 `/api/**` 访问本仓库服务。

## 验证

```powershell
.\mvnw.cmd test
docker compose ps
```
