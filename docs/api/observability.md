# 可观测性

## 请求追踪

所有基于 Spring MVC 的服务都会通过 `common` 模块自动注册请求追踪过滤器。

- 客户端可以通过 `X-Trace-Id` 传入追踪标识。
- 未传入时，服务会自动生成 UUID。
- 响应头会返回本次请求的 `X-Trace-Id`。
- 请求处理期间，追踪标识会写入日志 MDC 的 `traceId` 字段。
- 共享日志格式同时输出应用名和追踪标识，便于跨服务检索同一次请求。

Gateway 使用响应式认证过滤器生成或透传同名追踪头，并会清理客户端伪造的内部身份头。

## Prometheus 指标

服务引入 Micrometer Prometheus Registry，并暴露：

```text
/actuator/health
/actuator/info
/actuator/prometheus
```

每条指标都包含 `application` 标签，其值来自 `spring.application.name`。例如：

```text
application="tag-service"
```

生产环境可由 Prometheus 定期抓取各服务的 `/actuator/prometheus`。当前代码只负责暴露指标，不包含 Prometheus Server、Grafana 或告警平台的部署。

## 本地验证

启动任一服务后可执行：

```powershell
$response = Invoke-WebRequest -UseBasicParsing `
  -Uri 'http://localhost:8085/api/tags' `
  -Headers @{'X-Trace-Id' = 'local-trace-check'}

$response.Headers['X-Trace-Id']
Invoke-WebRequest -UseBasicParsing `
  -Uri 'http://localhost:8085/actuator/prometheus'
```

预期响应头返回 `local-trace-check`，指标文本中包含当前服务的 `application` 标签和 HTTP 请求指标。
