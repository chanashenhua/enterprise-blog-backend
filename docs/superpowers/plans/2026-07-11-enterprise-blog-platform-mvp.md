# 企业内部技术博客平台 MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个可本地运行的企业内部技术博客 MVP，跑通模拟企业登录、文章编辑发布、组织可见性、可选审核、文件上传、Elasticsearch 搜索和两个前端应用的核心闭环。

**Architecture:** 使用 Spring Cloud 微服务架构，按入口层、基础设施、身份权限域、内容域、支撑域拆分。第一阶段保留细粒度服务边界，但只实现可演示的主链路；通知、统计、审计不进入本计划交付范围。

**Tech Stack:** Nginx、Vue 3 + Vite、Spring Cloud Gateway、Eureka Server、Spring Cloud Config Server、Spring Boot、PostgreSQL、Redis、MinIO、Elasticsearch、Docker Compose、Maven、Java 17、Node 20。

## Global Constraints

- 前端必须拆成两个 Vue 应用：`web-portal` 和 `web-admin`。
- 登录按 OIDC/OAuth2 形态设计；本地实现使用 mock OIDC，不实现长期账号密码身份源。
- 权限模型使用 RBAC + 组织可见性策略，不实现完整 ABAC。
- 文章内容保存 `content_json`、`rendered_html`、`plain_text` 三份数据。
- 搜索使用 Elasticsearch，但 Elasticsearch 不能作为权限真相来源。
- 文件二进制内容存 MinIO，文件元数据存 `file_db`。
- PostgreSQL 使用一个实例多个 database，不允许跨 database join。
- 第一阶段不引入 RabbitMQ 或 Kafka，事件使用 Outbox/Event Table。
- Redis 只能作为缓存和计数缓冲，不能作为权限或文章内容真相来源。
- 每个后端服务必须暴露 Actuator health endpoint。
- 所有服务错误响应必须包含 `code`、`message`、`traceId`、`details`。

---

## Scope Check

原始设计覆盖多个子系统。本计划只交付“第一阶段核心闭环”，确保每个任务都能独立测试并逐步提交：

1. 项目骨架、Compose、配置中心和服务发现。
2. 通用后端契约：错误响应、用户上下文、traceId、outbox。
3. 身份、组织、权限最小闭环。
4. 文章、标签、审核、文件、搜索主链路。
5. 员工端和管理端最小可用 UI。
6. 端到端演示和测试。

## File Structure

```text
enterprise-blog-platform/
  pom.xml
  mvnw
  mvnw.cmd
  .mvn/
    wrapper/
      maven-wrapper.jar
      maven-wrapper.properties
  docker-compose.yml
  .env.example
  config-repo/
    gateway-service.yml
    eureka-server.yml
    config-server.yml
    user-service.yml
    org-service.yml
    permission-service.yml
    article-service.yml
    tag-service.yml
    review-service.yml
    file-service.yml
    search-service.yml
  backend/
    common/
    eureka-server/
    config-server/
    gateway-service/
    user-service/
    org-service/
    permission-service/
    article-service/
    tag-service/
    review-service/
    file-service/
    search-service/
  frontend/
    web-portal/
    web-admin/
  infra/
    nginx/
      nginx.conf
    postgres/
      init-databases.sql
    elasticsearch/
      article-index.json
    minio/
      buckets.md
  docs/
    api/
      error-codes.md
      local-demo.md
```

`backend/common` 只放跨服务稳定契约和基础设施，不放业务逻辑。每个业务服务拥有自己的数据库迁移、API、领域模型和测试。前端应用只通过 Gateway 的 `/api/**` 访问后端，不直接访问微服务、MinIO 或 Elasticsearch。

---

### Task 1: 项目骨架与本地基础设施

**Files:**
- Create: `pom.xml`
- Create: `backend/pom.xml`
- Create: `mvnw`
- Create: `mvnw.cmd`
- Create: `.mvn/wrapper/maven-wrapper.jar`
- Create: `.mvn/wrapper/maven-wrapper.properties`
- Create: `.env.example`
- Create: `docker-compose.yml`
- Create: `infra/postgres/init-databases.sql`
- Create: `infra/nginx/nginx.conf`
- Create: `infra/elasticsearch/article-index.json`
- Create: `infra/minio/buckets.md`
- Create: `docs/api/local-demo.md`

**Interfaces:**
- Produces: Maven 多模块工程，Docker Compose 服务名，PostgreSQL database 名称。
- Consumes: 无。

- [ ] **Step 1: 编写 Compose 配置验证测试**

在 `docs/api/local-demo.md` 写入本地启动约定：

```markdown
# 本地演示环境

## 服务地址

- web-portal: http://localhost:5173
- web-admin: http://localhost:5174
- gateway-service: http://localhost:8080
- eureka-server: http://localhost:8761
- config-server: http://localhost:8888
- minio console: http://localhost:9001
- elasticsearch: http://localhost:9200

## 验证命令

```powershell
docker compose config
docker compose up -d postgres redis minio elasticsearch
```
```

- [ ] **Step 2: 创建 Maven 多模块父 POM**

`pom.xml` 必须包含：

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.company.blog</groupId>
  <artifactId>enterprise-blog-platform</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <packaging>pom</packaging>

  <modules>
    <module>backend/common</module>
    <module>backend/eureka-server</module>
    <module>backend/config-server</module>
    <module>backend/gateway-service</module>
    <module>backend/user-service</module>
    <module>backend/org-service</module>
    <module>backend/permission-service</module>
    <module>backend/article-service</module>
    <module>backend/tag-service</module>
    <module>backend/review-service</module>
    <module>backend/file-service</module>
    <module>backend/search-service</module>
  </modules>

  <properties>
    <java.version>17</java.version>
    <spring-boot.version>3.3.5</spring-boot.version>
    <spring-cloud.version>2023.0.3</spring-cloud.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring-boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-dependencies</artifactId>
        <version>${spring-cloud.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>3.13.0</version>
          <configuration>
            <release>17</release>
          </configuration>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>3.5.2</version>
        </plugin>
        <plugin>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-maven-plugin</artifactId>
          <version>${spring-boot.version}</version>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
```

- [ ] **Step 3: 创建后端聚合 POM**

`backend/pom.xml` 必须包含：

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.company.blog</groupId>
    <artifactId>enterprise-blog-platform</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
  </parent>
  <artifactId>backend</artifactId>
  <packaging>pom</packaging>

  <modules>
    <module>common</module>
    <module>eureka-server</module>
    <module>config-server</module>
    <module>gateway-service</module>
    <module>user-service</module>
    <module>org-service</module>
    <module>permission-service</module>
    <module>article-service</module>
    <module>tag-service</module>
    <module>review-service</module>
    <module>file-service</module>
    <module>search-service</module>
  </modules>
</project>
```

- [ ] **Step 4: 创建 Maven Wrapper**

如果本机有 `mvn` 命令，运行：

```powershell
mvn -N wrapper:wrapper -Dmaven=3.9.9
```

如果本机没有 `mvn` 命令，使用受控下载方式生成 Maven Wrapper 文件。完成后必须存在：

```text
mvnw
mvnw.cmd
.mvn/wrapper/maven-wrapper.jar
.mvn/wrapper/maven-wrapper.properties
```

`.mvn/wrapper/maven-wrapper.properties` 必须包含：

```properties
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip
```

- [ ] **Step 5: 创建 PostgreSQL 初始化脚本**

`infra/postgres/init-databases.sql` 必须创建这些 database：

```sql
CREATE DATABASE user_db;
CREATE DATABASE org_db;
CREATE DATABASE permission_db;
CREATE DATABASE article_db;
CREATE DATABASE tag_db;
CREATE DATABASE review_db;
CREATE DATABASE file_db;
CREATE DATABASE search_db;
```

- [ ] **Step 6: 创建 Docker Compose 基础服务**

`docker-compose.yml` 必须至少包含 `postgres`、`redis`、`minio`、`elasticsearch`，并使用 healthcheck。验证命令：

```powershell
docker compose config
```

Expected: exit code `0`，输出包含 `postgres`、`redis`、`minio`、`elasticsearch`。

- [ ] **Step 7: 提交**

```powershell
git add pom.xml backend/pom.xml mvnw mvnw.cmd .mvn .env.example docker-compose.yml infra docs/api/local-demo.md
git commit -m "chore: scaffold platform infrastructure"
```

---

### Task 2: 后端 common 契约与可观测性

**Files:**
- Create: `backend/common/pom.xml`
- Create: `backend/common/src/main/java/com/company/blog/common/api/ApiError.java`
- Create: `backend/common/src/main/java/com/company/blog/common/api/ApiException.java`
- Create: `backend/common/src/main/java/com/company/blog/common/security/UserContext.java`
- Create: `backend/common/src/main/java/com/company/blog/common/web/TraceIdFilter.java`
- Create: `backend/common/src/main/java/com/company/blog/common/outbox/DomainEventStatus.java`
- Create: `backend/common/src/test/java/com/company/blog/common/api/ApiErrorTest.java`

**Interfaces:**
- Produces:
  - `ApiError(String code, String message, String traceId, Map<String, Object> details)`
  - `ApiException(String code, String message, int httpStatus)`
  - `UserContext(String userId, Set<String> roles, Set<String> departmentIds, Set<String> teamIds)`
  - `DomainEventStatus.PENDING | DELIVERED | FAILED`
- Consumes: Maven parent POM configuration from Task 1.

- [ ] **Step 1: 写失败测试**

`ApiErrorTest` 验证错误响应字段完整：

```java
@Test
void apiErrorContainsRequiredFields() {
    ApiError error = new ApiError(
        "ARTICLE_NOT_READABLE",
        "无权阅读该文章",
        "trace-1",
        Map.of("articleId", "a-1")
    );

    assertThat(error.code()).isEqualTo("ARTICLE_NOT_READABLE");
    assertThat(error.message()).isEqualTo("无权阅读该文章");
    assertThat(error.traceId()).isEqualTo("trace-1");
    assertThat(error.details()).containsEntry("articleId", "a-1");
}
```

- [ ] **Step 2: 运行失败测试**

```powershell
.\mvnw.cmd -pl backend/common test -Dtest=ApiErrorTest
```

Expected: FAIL，原因是 `ApiError` 尚不存在。

- [ ] **Step 3: 实现 common 契约**

创建 `ApiError`：

```java
package com.company.blog.common.api;

import java.util.Map;

public record ApiError(
    String code,
    String message,
    String traceId,
    Map<String, Object> details
) {}
```

创建 `UserContext`：

```java
package com.company.blog.common.security;

import java.util.Set;

public record UserContext(
    String userId,
    Set<String> roles,
    Set<String> departmentIds,
    Set<String> teamIds
) {}
```

- [ ] **Step 4: 运行测试**

```powershell
.\mvnw.cmd -pl backend/common test
```

Expected: PASS。

- [ ] **Step 5: 提交**

```powershell
git add backend/common
git commit -m "feat: add common api and security contracts"
```

---

### Task 3: Eureka、Config Server 与 Gateway 骨架

**Files:**
- Create: `backend/eureka-server/pom.xml`
- Create: `backend/eureka-server/src/main/java/com/company/blog/eureka/EurekaServerApplication.java`
- Create: `backend/config-server/pom.xml`
- Create: `backend/config-server/src/main/java/com/company/blog/config/ConfigServerApplication.java`
- Create: `backend/gateway-service/pom.xml`
- Create: `backend/gateway-service/src/main/java/com/company/blog/gateway/GatewayServiceApplication.java`
- Create: `backend/gateway-service/src/main/java/com/company/blog/gateway/security/MockOidcUserContextFilter.java`
- Create: `backend/gateway-service/src/test/java/com/company/blog/gateway/security/MockOidcUserContextFilterTest.java`
- Modify: `docker-compose.yml`
- Create: `config-repo/gateway-service.yml`
- Create: `config-repo/eureka-server.yml`
- Create: `config-repo/config-server.yml`

**Interfaces:**
- Produces: Gateway 对下游服务透传请求头：
  - `X-User-Id`
  - `X-User-Roles`
  - `X-Department-Ids`
  - `X-Team-Ids`
  - `X-Trace-Id`
- Consumes: `backend/common` 的 trace/user context 契约。

- [ ] **Step 1: 写 Gateway filter 测试**

测试 mock OIDC header 被转换为内部上下文 header：

```java
@Test
void addsUserContextHeadersFromMockOidcHeaders() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/articles")
        .header("X-Mock-User", "u-1")
        .header("X-Mock-Roles", "AUTHOR,READER")
        .header("X-Mock-Departments", "d-1")
        .header("X-Mock-Teams", "t-1")
        .build();

    MockOidcUserContextFilter filter = new MockOidcUserContextFilter();

    ServerWebExchange exchange = MockServerWebExchange.from(request);
    AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

    filter.filter(exchange, next -> {
        captured.set(next);
        return Mono.empty();
    }).block();

    HttpHeaders headers = captured.get().getRequest().getHeaders();
    assertThat(headers.getFirst("X-User-Id")).isEqualTo("u-1");
    assertThat(headers.getFirst("X-User-Roles")).isEqualTo("AUTHOR,READER");
}
```

- [ ] **Step 2: 运行失败测试**

```powershell
.\mvnw.cmd -pl backend/gateway-service test -Dtest=MockOidcUserContextFilterTest
```

Expected: FAIL，原因是 filter 尚不存在。

- [ ] **Step 3: 实现服务启动类与 Gateway filter**

`MockOidcUserContextFilter` 必须只在 `dev` profile 启用，并把 `X-Mock-*` 转为 `X-User-*`。

- [ ] **Step 4: 更新 Compose**

增加 `eureka-server`、`config-server`、`gateway-service` 服务。验证：

```powershell
docker compose config
.\mvnw.cmd -pl backend/eureka-server,backend/config-server,backend/gateway-service test
```

Expected: all tests PASS。

- [ ] **Step 5: 提交**

```powershell
git add backend/eureka-server backend/config-server backend/gateway-service config-repo docker-compose.yml
git commit -m "feat: add service discovery config and gateway"
```

---

### Task 4: 用户、组织、权限服务最小闭环

**Files:**
- Create: `backend/user-service/**`
- Create: `backend/org-service/**`
- Create: `backend/permission-service/**`
- Create: `backend/permission-service/src/main/java/com/company/blog/permission/api/PermissionCheckRequest.java`
- Create: `backend/permission-service/src/main/java/com/company/blog/permission/api/PermissionCheckResponse.java`
- Create: `backend/permission-service/src/test/java/com/company/blog/permission/PermissionPolicyTest.java`
- Create: `config-repo/user-service.yml`
- Create: `config-repo/org-service.yml`
- Create: `config-repo/permission-service.yml`

**Interfaces:**
- Produces:
  - `POST /internal/permissions/check`
  - Request: `userId`, `roles`, `departmentIds`, `teamIds`, `action`, `resourceType`, `resourceOwnerId`, `visibilityType`, `targetOrgIds`
  - Response: `allowed: boolean`, `reason: string`
- Consumes: Gateway user context headers.

- [ ] **Step 1: 写权限策略失败测试**

```java
@Test
void deniesTeamArticleWhenUserIsOutsideTargetTeam() {
    PermissionPolicy policy = new PermissionPolicy();
    PermissionDecision decision = policy.check(new PermissionCheckRequest(
        "u-1",
        Set.of("READER"),
        Set.of("d-1"),
        Set.of("t-1"),
        "article.read",
        "article",
        "u-2",
        "team",
        Set.of("t-2")
    ));

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.reason()).isEqualTo("USER_OUTSIDE_TARGET_ORG");
}
```

- [ ] **Step 2: 运行失败测试**

```powershell
.\mvnw.cmd -pl backend/permission-service test -Dtest=PermissionPolicyTest
```

Expected: FAIL，原因是 `PermissionPolicy` 尚不存在。

- [ ] **Step 3: 实现权限策略**

规则必须包含：

```text
article.read + company: authenticated user allowed
article.read + department: user departmentIds intersects targetOrgIds
article.read + team: user teamIds intersects targetOrgIds
article.publish: roles contains AUTHOR or ADMIN
article.review: roles contains REVIEWER or ADMIN
article.edit: resourceOwnerId == userId or roles contains ADMIN
```

- [ ] **Step 4: 创建用户和组织种子数据**

`user-service` 和 `org-service` 使用 Flyway migration 插入：

```text
u-admin: ADMIN, REVIEWER, AUTHOR, READER; department d-platform; team t-search
u-author: AUTHOR, READER; department d-platform; team t-search
u-reader: READER; department d-pay; team t-pay
```

- [ ] **Step 5: 验证服务测试**

```powershell
.\mvnw.cmd -pl backend/user-service,backend/org-service,backend/permission-service test
```

Expected: PASS。

- [ ] **Step 6: 提交**

```powershell
git add backend/user-service backend/org-service backend/permission-service config-repo
git commit -m "feat: add identity organization and permission services"
```

---

### Task 5: 文章、标签与 Outbox

**Files:**
- Create: `backend/article-service/**`
- Create: `backend/tag-service/**`
- Create: `backend/article-service/src/main/java/com/company/blog/article/api/SaveDraftRequest.java`
- Create: `backend/article-service/src/main/java/com/company/blog/article/api/SubmitPublishRequest.java`
- Create: `backend/article-service/src/main/java/com/company/blog/article/domain/ArticleStatus.java`
- Create: `backend/article-service/src/main/java/com/company/blog/article/domain/ArticleVisibilityType.java`
- Create: `backend/article-service/src/test/java/com/company/blog/article/ArticleStateMachineTest.java`
- Create: `backend/article-service/src/test/java/com/company/blog/article/ArticleContentProjectionTest.java`
- Create: `config-repo/article-service.yml`
- Create: `config-repo/tag-service.yml`

**Interfaces:**
- Produces:
  - `POST /api/articles/drafts`
  - `POST /api/articles/{articleId}/submit-publish`
  - `GET /api/articles/{articleId}`
  - Outbox event `ArticlePublished`
- Consumes:
  - `permission-service POST /internal/permissions/check`
  - `tag-service GET /internal/tags/validate`

- [ ] **Step 1: 写文章状态机失败测试**

```java
@Test
void companyArticlePublishesDirectly() {
    Article article = Article.draft("a-1", "u-author", "标题");
    article.submitForPublish(ArticleVisibilityType.COMPANY, Set.of(), false);

    assertThat(article.status()).isEqualTo(ArticleStatus.PUBLISHED);
    assertThat(article.pullEvents()).extracting(DomainEvent::type)
        .containsExactly("ArticlePublished");
}
```

- [ ] **Step 2: 写内容投影失败测试**

```java
@Test
void extractsPlainTextFromEditorJson() {
    String contentJson = """
        {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Redis 缓存策略"}]}]}
        """;

    ArticleContentProjection projection = ArticleContentProjection.from(contentJson);

    assertThat(projection.plainText()).isEqualTo("Redis 缓存策略");
    assertThat(projection.renderedHtml()).contains("<p>Redis 缓存策略</p>");
}
```

- [ ] **Step 3: 运行失败测试**

```powershell
.\mvnw.cmd -pl backend/article-service test -Dtest=ArticleStateMachineTest,ArticleContentProjectionTest
```

Expected: FAIL，原因是文章领域对象尚不存在。

- [ ] **Step 4: 实现文章领域与迁移**

文章状态必须包含：

```text
DRAFT
PENDING_REVIEW
PUBLISHED
WITHDRAWN
DELETED
```

迁移必须创建：

```text
article
article_content
article_visibility_target
article_publish_record
domain_event
```

- [ ] **Step 5: 验证测试**

```powershell
.\mvnw.cmd -pl backend/article-service,backend/tag-service test
```

Expected: PASS。

- [ ] **Step 6: 提交**

```powershell
git add backend/article-service backend/tag-service config-repo
git commit -m "feat: add article tag and outbox foundation"
```

---

### Task 6: 审核服务与部门/团队发布策略

**Files:**
- Create: `backend/review-service/**`
- Create: `backend/review-service/src/main/java/com/company/blog/review/api/EvaluateReviewPolicyRequest.java`
- Create: `backend/review-service/src/main/java/com/company/blog/review/api/EvaluateReviewPolicyResponse.java`
- Create: `backend/review-service/src/test/java/com/company/blog/review/ReviewPolicyTest.java`
- Modify: `backend/article-service/src/main/java/com/company/blog/article/**`
- Create: `config-repo/review-service.yml`

**Interfaces:**
- Produces:
  - `POST /internal/reviews/policies/evaluate`
  - `POST /internal/reviews/tickets`
  - `POST /api/admin/reviews/{ticketId}/approve`
  - `POST /api/admin/reviews/{ticketId}/reject`
- Consumes:
  - `permission-service POST /internal/permissions/check`
  - `article-service` publish callback or internal API.

- [ ] **Step 1: 写审核策略失败测试**

```java
@Test
void teamVisibilityRequiresReviewForConfiguredTeam() {
    ReviewPolicy policy = new ReviewPolicy(Set.of("t-search"));
    EvaluateReviewPolicyResponse response = policy.evaluate(
        new EvaluateReviewPolicyRequest("team", Set.of("t-search"))
    );

    assertThat(response.reviewRequired()).isTrue();
    assertThat(response.reviewerRole()).isEqualTo("REVIEWER");
}
```

- [ ] **Step 2: 运行失败测试**

```powershell
.\mvnw.cmd -pl backend/review-service test -Dtest=ReviewPolicyTest
```

Expected: FAIL。

- [ ] **Step 3: 实现审核策略与审核单**

审核单状态必须包含：

```text
PENDING
APPROVED
REJECTED
WITHDRAWN
```

- [ ] **Step 4: 连接 article-service**

`article-service` 发布部门/团队文章时调用 `review-service`：

```text
reviewRequired=false -> article.status=PUBLISHED
reviewRequired=true -> article.status=PENDING_REVIEW and review ticket created
```

- [ ] **Step 5: 验证测试**

```powershell
.\mvnw.cmd -pl backend/review-service,backend/article-service test
```

Expected: PASS。

- [ ] **Step 6: 提交**

```powershell
git add backend/review-service backend/article-service config-repo
git commit -m "feat: add review workflow for scoped articles"
```

---

### Task 7: 文件服务与 MinIO 受控访问

**Files:**
- Create: `backend/file-service/**`
- Create: `backend/file-service/src/main/java/com/company/blog/file/api/CreateUploadUrlRequest.java`
- Create: `backend/file-service/src/main/java/com/company/blog/file/api/CreateUploadUrlResponse.java`
- Create: `backend/file-service/src/test/java/com/company/blog/file/FileValidationTest.java`
- Modify: `docker-compose.yml`
- Create: `config-repo/file-service.yml`

**Interfaces:**
- Produces:
  - `POST /api/files/upload-url`
  - `GET /api/files/{fileId}/download-url`
  - `POST /internal/files/bind`
- Consumes:
  - MinIO bucket `blog-files`
  - Gateway user context headers.

- [ ] **Step 1: 写文件校验失败测试**

```java
@Test
void rejectsExecutableUpload() {
    FileValidation validation = new FileValidation(Set.of("image/png", "image/jpeg", "application/pdf"), 10 * 1024 * 1024);

    FileValidationResult result = validation.validate("script.exe", "application/x-msdownload", 1024);

    assertThat(result.allowed()).isFalse();
    assertThat(result.reason()).isEqualTo("FILE_TYPE_NOT_ALLOWED");
}
```

- [ ] **Step 2: 运行失败测试**

```powershell
.\mvnw.cmd -pl backend/file-service test -Dtest=FileValidationTest
```

Expected: FAIL。

- [ ] **Step 3: 实现文件元数据与 MinIO 客户端**

文件元数据必须包含：

```text
id
owner_id
object_key
original_name
content_type
size_bytes
created_at
```

- [ ] **Step 4: 验证受控访问**

运行：

```powershell
.\mvnw.cmd -pl backend/file-service test
```

Expected: PASS。测试必须覆盖不允许直接返回永久公开 URL。

- [ ] **Step 5: 提交**

```powershell
git add backend/file-service docker-compose.yml config-repo
git commit -m "feat: add controlled file service"
```

---

### Task 8: 搜索服务与权限安全过滤

**Files:**
- Create: `backend/search-service/**`
- Create: `backend/search-service/src/main/java/com/company/blog/search/api/SearchArticleRequest.java`
- Create: `backend/search-service/src/main/java/com/company/blog/search/api/SearchArticleResponse.java`
- Create: `backend/search-service/src/main/java/com/company/blog/search/index/ArticleSearchDocument.java`
- Create: `backend/search-service/src/test/java/com/company/blog/search/SearchVisibilityFilterTest.java`
- Modify: `infra/elasticsearch/article-index.json`
- Create: `config-repo/search-service.yml`

**Interfaces:**
- Produces:
  - `POST /internal/search/articles/index`
  - `DELETE /internal/search/articles/{articleId}`
  - `GET /api/search/articles`
- Consumes:
  - Elasticsearch index `articles-v1`
  - `permission-service POST /internal/permissions/check`

- [ ] **Step 1: 写搜索可见性失败测试**

```java
@Test
void filtersOutTeamArticleForUserOutsideTeam() {
    SearchVisibilityFilter filter = new SearchVisibilityFilter();
    UserContext user = new UserContext("u-reader", Set.of("READER"), Set.of("d-pay"), Set.of("t-pay"));
    ArticleSearchDocument doc = new ArticleSearchDocument("a-1", "team", Set.of("t-search"));

    assertThat(filter.isVisible(user, doc)).isFalse();
}
```

- [ ] **Step 2: 运行失败测试**

```powershell
.\mvnw.cmd -pl backend/search-service test -Dtest=SearchVisibilityFilterTest
```

Expected: FAIL。

- [ ] **Step 3: 实现索引文档与过滤规则**

`ArticleSearchDocument` 必须包含：

```text
articleId
title
summary
plainText
tags
authorId
authorName
visibilityType
targetOrgIds
status
publishedAt
updatedAt
```

- [ ] **Step 4: 连接 ArticlePublished outbox**

`article-service` dispatcher 调用 `search-service` 的索引接口。失败时 `domain_event.status` 保持 `PENDING` 或 `FAILED`，并增加 `retry_count`。

- [ ] **Step 5: 验证测试**

```powershell
.\mvnw.cmd -pl backend/search-service,backend/article-service test
```

Expected: PASS。

- [ ] **Step 6: 提交**

```powershell
git add backend/search-service backend/article-service infra/elasticsearch config-repo
git commit -m "feat: add permission aware article search"
```

---

### Task 9: 员工端 web-portal MVP

**Files:**
- Create: `frontend/web-portal/package.json`
- Create: `frontend/web-portal/vite.config.ts`
- Create: `frontend/web-portal/src/main.ts`
- Create: `frontend/web-portal/src/router.ts`
- Create: `frontend/web-portal/src/api/client.ts`
- Create: `frontend/web-portal/src/views/HomeView.vue`
- Create: `frontend/web-portal/src/views/ArticleEditorView.vue`
- Create: `frontend/web-portal/src/views/ArticleDetailView.vue`
- Create: `frontend/web-portal/src/views/SearchView.vue`
- Create: `frontend/web-portal/src/components/UserContextSwitcher.vue`

**Interfaces:**
- Produces: 员工端页面：
  - `/`
  - `/articles/new`
  - `/articles/:id`
  - `/search`
- Consumes: Gateway `/api/**`，使用 `X-Mock-*` headers 模拟登录。

- [ ] **Step 1: 写 API client 测试**

```ts
import { describe, expect, it } from "vitest";
import { buildMockUserHeaders } from "./client";

describe("buildMockUserHeaders", () => {
  it("builds dev mock identity headers", () => {
    expect(buildMockUserHeaders("u-author")).toEqual({
      "X-Mock-User": "u-author",
      "X-Mock-Roles": "AUTHOR,READER",
      "X-Mock-Departments": "d-platform",
      "X-Mock-Teams": "t-search",
    });
  });
});
```

- [ ] **Step 2: 运行失败测试**

```powershell
cd frontend/web-portal
npm test -- --run
```

Expected: FAIL。

- [ ] **Step 3: 实现员工端最小 UI**

页面必须支持：

```text
切换 mock 用户
保存草稿
选择 company/department/team 可见性
提交发布
查看文章详情
搜索文章
```

- [ ] **Step 4: 验证前端**

```powershell
cd frontend/web-portal
npm run build
npm test -- --run
```

Expected: PASS。

- [ ] **Step 5: 提交**

```powershell
git add frontend/web-portal
git commit -m "feat: add employee portal mvp"
```

---

### Task 10: 管理端 web-admin MVP

**Files:**
- Create: `frontend/web-admin/package.json`
- Create: `frontend/web-admin/vite.config.ts`
- Create: `frontend/web-admin/src/main.ts`
- Create: `frontend/web-admin/src/router.ts`
- Create: `frontend/web-admin/src/api/client.ts`
- Create: `frontend/web-admin/src/views/DashboardView.vue`
- Create: `frontend/web-admin/src/views/ReviewQueueView.vue`
- Create: `frontend/web-admin/src/views/SearchIndexTasksView.vue`
- Create: `frontend/web-admin/src/views/TagManagementView.vue`

**Interfaces:**
- Produces: 管理端页面：
  - `/`
  - `/reviews`
  - `/search-tasks`
  - `/tags`
- Consumes:
  - `GET /api/admin/reviews`
  - `POST /api/admin/reviews/{ticketId}/approve`
  - `POST /api/admin/reviews/{ticketId}/reject`
  - `GET /api/admin/search/tasks`

- [ ] **Step 1: 写审核页面测试**

```ts
import { describe, expect, it } from "vitest";
import { reviewStatusLabel } from "./reviewLabels";

describe("reviewStatusLabel", () => {
  it("renders pending status in Chinese", () => {
    expect(reviewStatusLabel("PENDING")).toBe("待审核");
  });
});
```

- [ ] **Step 2: 运行失败测试**

```powershell
cd frontend/web-admin
npm test -- --run
```

Expected: FAIL。

- [ ] **Step 3: 实现管理端最小 UI**

页面必须支持：

```text
查看待审核文章
通过审核
拒绝审核并填写意见
查看搜索索引失败任务
重试索引任务
管理标签列表
```

- [ ] **Step 4: 验证前端**

```powershell
cd frontend/web-admin
npm run build
npm test -- --run
```

Expected: PASS。

- [ ] **Step 5: 提交**

```powershell
git add frontend/web-admin
git commit -m "feat: add admin portal mvp"
```

---

### Task 11: Nginx、Compose 全链路与种子数据

**Files:**
- Modify: `docker-compose.yml`
- Modify: `infra/nginx/nginx.conf`
- Create: `infra/postgres/seed-demo-data.sql`
- Modify: `docs/api/local-demo.md`

**Interfaces:**
- Produces: 一条可演示链路：
  - `u-author` 写公司可见文章并搜索到。
  - `u-author` 写团队可见文章进入审核。
  - `u-admin` 审核通过。
  - `u-reader` 搜索不到 `t-search` 团队可见文章。
- Consumes: 所有前面任务的服务和前端应用。

- [ ] **Step 1: 写本地演示验收脚本说明**

在 `docs/api/local-demo.md` 补充固定演示步骤：

```markdown
## 核心验收

1. 使用 u-author 创建公司可见文章。
2. 在搜索页搜索标题，能看到该文章。
3. 使用 u-author 创建 t-search 团队可见文章。
4. 文章进入待审核。
5. 使用 u-admin 在管理端审核通过。
6. 使用 u-reader 搜索相同标题，看不到该团队文章。
7. 使用 u-author 搜索相同标题，可以看到该团队文章。
```

- [ ] **Step 2: 更新 Compose 服务**

Compose 必须启动：

```text
nginx
web-portal
web-admin
gateway-service
eureka-server
config-server
user-service
org-service
permission-service
article-service
tag-service
review-service
file-service
search-service
postgres
redis
minio
elasticsearch
```

- [ ] **Step 3: 验证 Compose 配置**

```powershell
docker compose config
```

Expected: exit code `0`。

- [ ] **Step 4: 启动全链路**

```powershell
docker compose up -d
docker compose ps
```

Expected: 后端服务、PostgreSQL、Redis、MinIO、Elasticsearch 均为 healthy 或 running。

- [ ] **Step 5: 提交**

```powershell
git add docker-compose.yml infra docs/api/local-demo.md
git commit -m "chore: wire full local demo environment"
```

---

### Task 12: 端到端测试与风险场景

**Files:**
- Create: `frontend/e2e/package.json`
- Create: `frontend/e2e/playwright.config.ts`
- Create: `frontend/e2e/tests/article-publish-search.spec.ts`
- Create: `frontend/e2e/tests/scoped-visibility.spec.ts`
- Modify: `docs/api/error-codes.md`
- Modify: `docs/api/local-demo.md`

**Interfaces:**
- Produces: Playwright E2E 测试。
- Consumes: Task 11 的本地全链路环境。

- [ ] **Step 1: 写文章发布搜索 E2E**

`article-publish-search.spec.ts` 必须执行：

```ts
test("author publishes company article and finds it in search", async ({ page }) => {
  await page.goto("http://localhost:5173");
  await page.getByRole("combobox", { name: "用户" }).selectOption("u-author");
  await page.getByRole("link", { name: "写文章" }).click();
  await page.getByLabel("标题").fill("Redis 缓存策略");
  await page.getByLabel("正文").fill("Redis 用于权限摘要缓存和计数缓冲。");
  await page.getByLabel("可见性").selectOption("company");
  await page.getByRole("button", { name: "发布" }).click();
  await page.getByRole("link", { name: "搜索" }).click();
  await page.getByRole("textbox", { name: "搜索文章" }).fill("Redis 缓存策略");
  await page.getByRole("button", { name: "搜索" }).click();
  await expect(page.getByText("Redis 缓存策略")).toBeVisible();
});
```

- [ ] **Step 2: 写可见性 E2E**

`scoped-visibility.spec.ts` 必须证明非目标团队用户搜索不到团队文章。

- [ ] **Step 3: 运行失败测试**

```powershell
cd frontend/e2e
npm test
```

Expected: 在 E2E 页面和接口未完整连接前 FAIL。

- [ ] **Step 4: 修复 E2E 暴露的问题**

修复必须只围绕：

```text
登录上下文 header
文章发布状态
审核状态
搜索索引同步
可见性过滤
页面无障碍 label
```

- [ ] **Step 5: 运行完整验证**

```powershell
.\mvnw.cmd test
cd frontend/web-portal; npm run build; npm test -- --run
cd ../web-admin; npm run build; npm test -- --run
cd ../e2e; npm test
docker compose ps
```

Expected: 所有测试 PASS，Compose 服务 running 或 healthy。

- [ ] **Step 6: 提交**

```powershell
git add frontend/e2e frontend/web-portal frontend/web-admin backend docs/api
git commit -m "test: add end to end coverage for article workflow"
```

---

## Final Verification

实现完成后运行：

```powershell
.\mvnw.cmd test
docker compose config
docker compose up -d
docker compose ps
cd frontend/web-portal; npm run build; npm test -- --run
cd ../web-admin; npm run build; npm test -- --run
cd ../e2e; npm test
```

成功标准：

- 所有 Maven 测试通过。
- 两个前端应用 build 和 unit tests 通过。
- E2E 测试通过。
- 本地 Compose 环境能启动核心服务。
- 公司可见文章可搜索。
- 团队可见文章只对目标团队可搜索。
- 审核通过后文章进入可搜索状态。
- 文件上传走 `file-service`，不暴露永久公开 MinIO URL。

## Commit Strategy

每个 Task 单独提交。提交信息使用：

- `chore:` 基础设施、配置、Compose。
- `feat:` 用户可见能力或服务能力。
- `test:` 测试覆盖。
- `docs:` 文档补充。

## Plan Self-Review

Spec 覆盖检查：

- 企业 OIDC/OAuth2：Task 3、Task 4。
- RBAC + 组织可见性：Task 4、Task 8、Task 12。
- 文章内容三份存储：Task 5。
- 审核策略：Task 6、Task 10、Task 12。
- 文件与 MinIO：Task 7。
- Elasticsearch 搜索：Task 8、Task 12。
- Docker Compose：Task 1、Task 11。
- 两个 Vue 应用：Task 9、Task 10。
- Outbox/Event Table：Task 5、Task 8。
- 错误响应、traceId、健康检查：Task 2、Task 3、Task 11。

占位词扫描结果：未发现未完成标记或空泛步骤。

类型一致性检查：

- 用户上下文统一使用 `UserContext(userId, roles, departmentIds, teamIds)`。
- 可见性统一使用 `company`、`department`、`team`。
- 文章状态统一使用 `DRAFT`、`PENDING_REVIEW`、`PUBLISHED`、`WITHDRAWN`、`DELETED`。
- 审核状态统一使用 `PENDING`、`APPROVED`、`REJECTED`、`WITHDRAWN`。
- 搜索文档统一使用 `ArticleSearchDocument`。
