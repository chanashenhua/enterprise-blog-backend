# 第一阶段完成状态

更新时间：2026-07-25

## 结论

第一阶段的后端非部署开发范围已经完成并通过代码级验证；员工端和管理端现有代码也已通过测试、类型检查和生产构建。

第一阶段整体暂不标记为 100% 完成。当前没有安装 Docker，按约定未启动 Nginx、Redis、MinIO、Elasticsearch 和 Compose 微服务集群，因此全链路端到端验收与部署验收仍待执行。

## 已完成

### 后端业务能力

- 文章聚合已由内存仓储切换为 PostgreSQL，服务重启后数据不会丢失。
- 已支持文章草稿、编辑、提交、审核状态回调、发布、撤回、软删除和内容版本。
- 已支持分类、标签管理，以及文章按分类和标签筛选。
- 已支持文章评论、一级回复、编辑和软删除。
- 已实现 Gateway 开发环境 Mock OIDC 与生产环境 JWT/OIDC 校验的配置隔离。
- 已实现 Redis 分类和标签缓存；无 Redis 的本机配置使用进程内缓存。
- 已实现请求 `X-Trace-Id` 生成或透传、日志 MDC 注入和 Prometheus 指标端点。
- PostgreSQL 本机配置、迁移和可重复执行的演示数据脚本已补齐。

### 本机演示数据

当前本机 PostgreSQL 演示数据包括：

- 3 篇文章；
- 3 个文章内容版本；
- 2 条评论；
- 5 个启用标签；
- 4 个启用分类。

数据库密码只通过临时环境变量传入，未写入仓库。

### 验证结果

- Maven Reactor 的 14 个模块全部构建成功。
- 后端共执行 102 个测试，失败 0、错误 0、跳过 0。
- 实际启动 `tag-service` 后，健康检查为 `UP`。
- 实际请求能原样返回传入的 `X-Trace-Id`。
- `/actuator/prometheus` 包含服务名标签和 HTTP 请求指标。
- `web-portal`：1 个测试通过，TypeScript 类型检查和 Vite 生产构建通过。
- `web-admin`：1 个测试通过，TypeScript 类型检查和 Vite 生产构建通过。
- 前端仓库已创建并推送到 GitHub，`main` 保存当前基线，后续开发使用 `codex/develop`。
- 后端草稿 PR 可干净合并，当前没有配置 GitHub CI 检查。

## 已独立提交的大模块

| 提交 | 模块 |
| --- | --- |
| `9454af6` | PostgreSQL 文章聚合仓储 |
| `f41f515` | 完整文章生命周期 |
| `369dc21` | 文章评论与回复 |
| `d727ff9` | 标签与分类管理 |
| `5678a98` | Gateway OIDC/JWT 认证 |
| `dd2b551` | Redis 分类与标签缓存 |
| `c3c1691` | Trace ID 与 Prometheus 可观测性 |

## 暂缓项

以下事项需要 Docker、外部服务或额外仓库权限，当前按用户要求不执行部署：

1. 启动并验证 Nginx、Gateway、Eureka、Config Server 和全部微服务。
2. 验证 PostgreSQL 多数据库、Redis、MinIO、Elasticsearch 的 Compose 运行链路。
3. 执行“登录、写文章、上传图片、提交审核、审核通过、搜索、评论”的浏览器端到端测试。
4. 使用真实企业 OIDC Issuer、Client 和 Claims 做运行时联调。

## 后续验收顺序

1. 后续前端功能提交到 `codex/develop`，通过 PR 合并到 `main`。
2. 本机具备 Docker 后启动基础设施和全套服务，不直接部署生产环境。
3. 执行端到端流程与风险场景测试，修复发现的问题。
4. 更新草稿 PR 的最终验证记录，转为 Ready for review，再合并到 `main`。
