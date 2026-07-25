package com.company.blog.article.api;

import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

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

    @PutMapping("/{articleId}/draft")
    /** 修改草稿；撤回的文章修改后会重新进入草稿状态。 */
    public ArticleResponse updateDraft(
            @PathVariable("articleId") String articleId,
            @RequestHeader HttpHeaders headers,
            @RequestBody UpdateDraftRequest request
    ) {
        return articleService.updateDraft(articleId, CallerContext.from(headers), request);
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

    @PostMapping("/{articleId}/withdraw")
    /** 撤回已发布文章，并通过 Outbox 异步移除搜索索引。 */
    public ArticleResponse withdraw(
            @PathVariable("articleId") String articleId,
            @RequestHeader HttpHeaders headers
    ) {
        return articleService.withdraw(articleId, CallerContext.from(headers));
    }

    @DeleteMapping("/{articleId}")
    /** 软删除文章，保留治理和审计所需的数据库记录。 */
    public ArticleResponse delete(
            @PathVariable("articleId") String articleId,
            @RequestHeader HttpHeaders headers
    ) {
        return articleService.delete(articleId, CallerContext.from(headers));
    }

    @GetMapping("/mine")
    /** 查询当前用户未删除的草稿、待审核、已发布和已撤回文章。 */
    public List<ArticleResponse> listMine(@RequestHeader HttpHeaders headers) {
        return articleService.listMine(CallerContext.from(headers));
    }

    @GetMapping("/{articleId}/versions")
    /** 仅作者或管理员可查看文章内容版本。 */
    public List<ArticleContentVersion> listVersions(
            @PathVariable("articleId") String articleId,
            @RequestHeader HttpHeaders headers
    ) {
        return articleService.listVersions(articleId, CallerContext.from(headers));
    }

    @GetMapping("/{articleId}")
    /** 读取文章当前状态；已发布文章按可见范围鉴权，其他状态只允许作者或管理员查看。 */
    public ArticleResponse get(
            @PathVariable("articleId") String articleId,
            @RequestHeader HttpHeaders headers
    ) {
        return articleService.get(articleId, CallerContext.from(headers));
    }
}
