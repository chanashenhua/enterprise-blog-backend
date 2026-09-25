# IDEA 与 WebStorm 本地启动指南

本指南适用于不安装 Docker 的本地开发环境。后端使用 Java 17、本机 PostgreSQL 和 Redis；Elasticsearch、MinIO、Nginx 与正式部署不属于当前阶段。

## 1. IDEA 基础配置

1. 将 Project SDK、Project language level、所有模块 SDK 和 Maven Runner JRE 设置为 JDK 17。
2. 从根目录 `pom.xml` 重新加载 Maven 项目，不要逐个导入子模块。
3. Config Server 环境变量：

```text
SPRING_PROFILES_ACTIVE=native
SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS=file:///$PROJECT_DIR$/config-repo
SERVER_PORT=8888
```

4. Gateway 环境变量：

```text
SPRING_PROFILES_ACTIVE=dev
SPRING_CONFIG_IMPORT=configserver:http://localhost:8888
SPRING_CLOUD_CONFIG_FAIL_FAST=true
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://localhost:8761/eureka/
GATEWAY_DEV_MOCK_TOKEN=local-dev-token
```

5. 需要数据库的业务服务使用 `SPRING_PROFILES_ACTIVE=local`，并设置：

```text
SPRING_CONFIG_IMPORT=configserver:http://localhost:8888
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://localhost:8761/eureka/
POSTGRES_PASSWORD=<你的本机 PostgreSQL 密码>
```

## 2. 后端启动顺序

按以下顺序启动：

1. `EurekaServerApplication`（8761）
2. `ConfigServerApplication`（8888）
3. `OrgServiceApplication`（8082）、`PermissionServiceApplication`（8083）
4. `ArticleServiceApplication`（8084）、`TagServiceApplication`（8085）、`ReviewServiceApplication`（8086）
5. `CommentServiceApplication`（8089）、`StatsServiceApplication`（8090）、`NotificationServiceApplication`（8091）、`AuditServiceApplication`（8092）
6. `GatewayServiceApplication`（8080）

访问 `http://localhost:8761` 确认服务已注册，再启动前端。搜索和文件功能分别依赖 Elasticsearch 与 MinIO，本地没有这些基础设施时可以暂不启动对应服务。

## 3. WebStorm 启动前端

前端仓库位于相邻目录 `enterprise-blog-frontend`，包含员工端 `web-portal` 和管理端 `web-admin`。两个应用分别创建 npm 配置，命令均为 `dev`，并设置以下环境变量：

```text
VITE_AUTH_MODE=local
VITE_MOCK_OIDC_TOKEN=local-dev-token
VITE_API_PROXY_TARGET=http://localhost:8080
```

管理端可额外设置：

```text
VITE_PORTAL_URL=http://localhost:5173
```

员工端可额外设置：

```text
VITE_ADMIN_URL=http://localhost:5174
```

启动地址：

| 应用 | 目录 | 地址 |
| --- | --- | --- |
| 员工端 | `web-portal` | `http://localhost:5173` |
| 管理端 | `web-admin` | `http://localhost:5174` |

首次访问会进入登录页，可选择三个本地演示账号：

| 账号 | 身份 | 主要角色 |
| --- | --- | --- |
| `u-admin` | 平台管理员 | `ADMIN, REVIEWER, AUTHOR, READER` |
| `u-author` | 技术作者 | `AUTHOR, READER` |
| `u-reader` | 企业读者 | `READER` |

登录无需密码。管理端允许三种身份完成验证，但只有 `u-admin` 能加载管理页面和管理接口；其他身份会进入无权限页。员工端与管理端会话分别保存在各自的 `localStorage`，只保存版本号和账号 ID，不保存模拟令牌。

## 4. 重要安全说明

本地演示登录仅用于开发和验收，不是生产认证方案。`VITE_MOCK_OIDC_TOKEN` 必须与 Gateway 的 `GATEWAY_DEV_MOCK_TOKEN` 一致，且都不能作为真实密钥提交或用于生产。

生产环境必须使用企业 OIDC 和 Bearer JWT，不能启用 `dev` Profile，也不会显示演示账号。

## 5. 启动检查

- Gateway 健康检查：`http://localhost:8080/actuator/health`
- 未登录访问任意业务路径时应跳转到 `/login?redirect=...`
- 登录后刷新页面，会话应保持
- 管理端使用 `u-author` 或 `u-reader` 时只显示无权限页
- 如果接口返回 500，先检查 Gateway 日志中的目标服务名、Eureka 注册情况、数据库连接和 Redis 状态

IDEA 和 WebStorm 的个人运行配置可能包含本机密码，应保留在本地 `.idea/workspace.xml`，不要提交到 Git。

## 6. Markdown 编辑器启动检查

更新本次代码后，在 IDEA 重新加载 Maven（文章服务新增 CommonMark 依赖），重新启动 `ArticleServiceApplication`。前端开发服务会自动更新，生产构建需要重新构建。

使用 `u-author` 或 `u-admin` 进入「写文章」，测试标题、正文、对照预览、分类标签选择和保存后再次编辑。预览与保存依赖文章服务，分类标签依赖标签服务，并通过 Gateway 请求；只启动前端不能完成这些功能。

当前编辑器尚未自动保存，离开前请保存草稿。组织范围支持按名称搜索多选；不会因为保存草稿而改变发布范围。详见 [文章编辑器接口](api/article-editor.md) 和 [第五阶段计划](phase-5-plan.md)。

## 7. 组织范围选择启动检查

本次更新后重新加载 Maven，重启 Config Server、Gateway、Permission Service 和 Article Service，并启动 `OrgServiceApplication`（8082，`local` Profile，使用上面的数据库环境变量）。组织服务读取既有部门／团队数据，本模块没有新增数据库迁移或修改测试数据。

`org-service-local.yml` 和 `article-service-local.yml` 已使用相同的本地开发内部令牌 `local-org-token`。若自定义 `INTERNAL_ORG_TOKEN`，两服务必须设置相同的值。非本地环境必须配置独立密钥，不能沿用这个公开的演示值。文章服务本地通过 `http://localhost:8082` 校验组织，前端仍只请求 Gateway。

以作者身份进入编辑器，选择「指定团队」，应显示本人所属的 Search Team；管理员可选择全部现存部门和团队。提交时仍由后端核对作者身份、组织范围并执行原审核策略。组织服务不可用时，指定组织发布会被阻止，但正文可继续保存草稿。接口与安全边界见 [组织范围说明](api/organization-scope.md)。
