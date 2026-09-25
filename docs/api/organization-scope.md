# 文章发布组织范围

更新时间：2026-09-25。

## 组织目录

`GET /api/organizations/publish-options` 通过 Gateway 调用 `org-service`，读取现有 `department`、`team` 表，不新增表、不写业务数据。

```json
{
  "departments": [{"id": "d-platform", "name": "Platform Engineering"}],
  "teams": [{"id": "t-search", "name": "Search Team", "departmentId": "d-platform", "departmentName": "Platform Engineering"}]
}
```

- 必须已认证，且有 AUTHOR 或 ADMIN 角色；未认证返回 401，其他角色返回 403。
- ADMIN 获得全部现存组织；AUTHOR 仅获得可信身份中的部门、团队与数据库现存记录的交集。团队结果包含所属部门名称，但所属团队不自动授予向整个部门发布的权限。
- 身份来自 Gateway 清洗、校验并转发的 `X-User-Id`、`X-User-Roles`、`X-Department-Ids`、`X-Team-Ids`。请求参数不能覆盖身份。
- 无可选组织返回空数组。名称来自数据库，按名称、ID 排序；不在前端硬编码组织名称。
- 服务端口仅供可信内网访问，不可绕过 Gateway 对外开放；本地 Mock 身份仅用于开发。

## 发布校验

沿用 `POST /api/articles/{id}/submit-publish`，例如：

```json
{"visibilityType": "TEAM", "targetOrgIds": ["t-search"], "reviewRequired": true}
```

1. 需要创作角色。AUTHOR 只能发布本人文章；ADMIN 可操作其他作者的文章。
2. COMPANY 必须不带目标组织；DEPARTMENT、TEAM 必须选择 1～50 个目标，ID 非空、无首尾空格、最长 64 字符。
3. AUTHOR 必须属于每一个目标部门／团队，而非只匹配任意一个；ADMIN 可跨组织发布，但目标仍必须真实存在。
4. 文章服务调用组织服务重新验证全部目标存在且类型正确，之后才进入原有审核／发布流程。
5. 非法范围或不存在的目标返回 400；无操作权限返回 403；组织校验不可用、未配置或响应无效返回 503。失败时不修改文章状态、不创建审核单或 Outbox。

全公司发布无需组织服务在线。范围发布继续遵循已有审核策略；本模块不修改审核通过／驳回协议。组织存在性校验发生在提交时，不是跨服务数据库事务，也不代表后续组织删除时自动撤回历史文章或审核单。

### 内部校验接口

`POST /internal/organizations/validate-targets` 使用同样的 `visibilityType`、`targetOrgIds` 字段，返回 `{"valid":true}` 或 `{"valid":false}`。调用必须携带 `X-Internal-Org-Token`。Gateway 不开放此路由；它仅验证组织存在性，发布授权仍由权限服务负责。

组织服务与文章服务的 `INTERNAL_ORG_TOKEN` 必须一致。非本地配置默认空值并拒绝内部调用，不能使用空令牌放行。`local` Profile 使用公开的开发值 `local-org-token`，只适用于本机演示；其他环境必须注入独立密钥，不能提交真实密钥。

## 编辑器行为

- 按部门／团队名称或 ID 搜索、多选，团队也支持按所属部门搜索；摘要展示当前选中组织名称。
- 切换 COMPANY、DEPARTMENT、TEAM 时清空旧目标，防止不同类型的 ID 混用。
- 恢复文章或刷新目录后，已删除、已不在当前身份权限内的目标保留并标记，要求显式移除，不静默丢失或放行。
- 目录失败可重试，保留正文，允许保存草稿；在组织有效性未确认前禁用范围发布。
- 当前组织表没有启用／停用字段，因此“失效”指记录不存在或当前身份不再有对应权限，不表示实现了组织停用管理。
- 保存草稿仍只保存正文、分类、标签；可见范围仅在提交发布时生效，当前没有自动保存发布设置。

## 验证边界

组织目录 MockMvc/JDBC 测试使用 H2 PostgreSQL 兼容模式，覆盖真实 SQL 查询、身份过滤和删除记录后的校验。权限及文章服务测试覆盖越权、服务故障和失败无副作用。浏览器组织选择器测试使用 API 测试桩，不能替代实际后端服务联调。
