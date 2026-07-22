package com.company.blog.article.api;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
/**
 * 面向文章作者的 HTTP 入口。
 *
 * <p>这里仅接收网关传递的用户上下文并调用应用服务；文章状态、权限和跨服务协作
 * 都由 {@link ArticleService} 处理，避免 Controller 变成业务规则的存放处。</p>
 */
public class ArticleController {
    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping("/drafts")
    /** 保存一篇草稿。作者身份来自网关注入的 {@code X-User-Id}，不接受客户端请求体伪造。 */
    public ArticleResponse saveDraft(
            @RequestHeader("X-User-Id") String authorId,
            @RequestBody SaveDraftRequest request
    ) {
        return articleService.saveDraft(authorId, request);
    }

    @PostMapping("/{articleId}/submit-publish")
    /**
     * 提交发布申请。
     * 网关会把登录用户的角色和组织范围写入请求头，文章服务据此进行发布权限校验。
     */
    public ArticleResponse submitForPublish(
            @PathVariable("articleId") String articleId,
            @RequestHeader HttpHeaders headers,
            @RequestBody SubmitPublishRequest request
    ) {
        return articleService.submitForPublish(articleId, CallerContext.from(headers), request);
    }

    @GetMapping("/{articleId}")
    /** 读取文章当前状态；MVP 阶段用于作者及审核流程查询。 */
    public ArticleResponse get(@PathVariable("articleId") String articleId) {
        return articleService.get(articleId);
    }
}
