# 网关身份认证

下游服务只信任网关注入的用户上下文头：

- `X-User-Id`
- `X-User-Roles`
- `X-Department-Ids`
- `X-Team-Ids`

网关会先删除客户端自行携带的这些头，再根据当前认证模式重新生成。

## 开发模式

`dev` Profile 使用本地身份模拟器。请求必须携带正确的 `X-Mock-Token`，并可通过
`X-Mock-User`、`X-Mock-Roles`、`X-Mock-Departments`、`X-Mock-Teams` 提供演示身份。
模拟令牌来自 `GATEWAY_DEV_MOCK_TOKEN`，不能提交到 Git。

## 生产 JWT/OIDC 模式

非 `dev` Profile 会启用 OAuth 2.0 Resource Server。除健康检查外，所有网关请求都必须
携带 `Authorization: Bearer <JWT>`，JWT 由配置的 OIDC issuer 公钥验签。

生产环境至少配置：

```text
SPRING_PROFILES_ACTIVE=prod
OIDC_ISSUER_URI=https://identity.example.com/realms/company
```

身份平台的 claim 名可以覆盖：

```text
OIDC_USER_ID_CLAIM=sub
OIDC_ROLES_CLAIM=realm_access.roles
OIDC_DEPARTMENTS_CLAIM=department_ids
OIDC_TEAMS_CLAIM=team_ids
```

claim 路径支持点号访问嵌套对象。角色会统一转为大写，并去掉可选的 `ROLE_` 前缀。
部门和团队 claim 可以是数组或逗号分隔字符串。网关生成下游可信头后会移除原始 Bearer
令牌和全部 `X-Mock-*` 头，避免凭据在内部服务间继续传播。

`/actuator/health` 在生产模式下允许匿名访问，其他路由默认全部要求认证。实际接入身份平台时
仍需由部署环境提供 issuer、网络信任链和对应的 claim 映射；仓库不保存客户端密钥。
