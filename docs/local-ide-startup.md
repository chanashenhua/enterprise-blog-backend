# IDEA 与 WebStorm 本地启动指南

本指南用于当前不安装 Docker 的开发环境。后端统一使用 Java 17，本机 PostgreSQL 和 Redis 需要先启动；
Elasticsearch、MinIO、Nginx 和正式部署不属于当前阶段。

## 一、IDEA 基础设置

1. 将 Project SDK、Project language level、所有模块 SDK 和 Maven Runner JRE 都设为 JDK 17。
2. 重新加载根目录 `pom.xml`，不要逐个导入子模块。
3. 每个业务服务使用 `SPRING_CONFIG_IMPORT=configserver:http://localhost:8888`。
4. 需要数据库的服务使用 `SPRING_PROFILES_ACTIVE=local`，并通过运行配置传入
   `POSTGRES_PASSWORD`；密码不要写入仓库。
5. 所有业务服务使用
   `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://localhost:8761/eureka/`。

Config Server 的运行配置使用：

```text
SPRING_PROFILES_ACTIVE=native
SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS=file:///$PROJECT_DIR$/config-repo
SERVER_PORT=8888
```

Gateway 的运行配置使用：

```text
SPRING_PROFILES_ACTIVE=dev
SPRING_CONFIG_IMPORT=configserver:http://localhost:8888
SPRING_CLOUD_CONFIG_FAIL_FAST=true
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://localhost:8761/eureka/
GATEWAY_DEV_MOCK_TOKEN=local-dev-token
```

## 二、启动顺序

先启动基础入口，再启动业务服务：

1. `EurekaServerApplication`（8761）；
2. `ConfigServerApplication`（8888）；
3. `PermissionServiceApplication`（8083）；
4. `ArticleServiceApplication`（8084）、`TagServiceApplication`（8085）、
   `ReviewServiceApplication`（8086）；
5. `CommentServiceApplication`（8089）、`StatsServiceApplication`（8090）、
   `NotificationServiceApplication`（8091）、`AuditServiceApplication`（8092）；
6. `GatewayServiceApplication`（8080）。

以上服务可验收第四阶段的首页、评论、分类标签、专题和管理概览。搜索依赖 Elasticsearch，文件上传依赖
MinIO；本机没有对应基础设施时，可以不启动 `SearchServiceApplication` 和 `FileServiceApplication`，
相关页面功能会暂不可用。需要验证这些能力时再补齐对应环境。

在浏览器打开 `http://localhost:8761`，确认上面启动的业务服务均已注册，然后再启动前端。

## 三、WebStorm 启动前端

在相邻的 `enterprise-blog-frontend` 仓库分别创建两个 npm 运行配置：

| 页面 | package.json | 命令 | 地址 |
| --- | --- | --- | --- |
| 员工前台 | `web-portal/package.json` | `dev` | `http://localhost:5173` |
| 管理端 | `web-admin/package.json` | `dev` | `http://localhost:5174` |

两个 Vite 开发服务器默认把 `/api` 代理到 `http://localhost:8080`，无需再配置跨域地址。

首次运行前分别在 `web-portal` 和 `web-admin` 目录执行 `npm install`。日常启动时，直接运行 WebStorm
保存的 npm 配置即可。

## 四、启动检查

- Gateway 健康检查：`http://localhost:8080/actuator/health`；
- 员工前台首页能加载真实文章、分类、标签和专题；
- 管理端首页能展示内容、互动、评论、订阅和通知概览；
- 如果接口返回 500，优先查看 Gateway 日志中的目标服务名，再确认该服务是否已注册、是否使用 `local`
  Profile，以及 PostgreSQL 密码环境变量是否存在。

IDEA 的个人运行配置可能包含本机密码，因此保留在本地 `.idea/workspace.xml`，不会提交到 Git。
