package com.company.blog.search.api;

import com.company.blog.search.index.ArticleSearchDocument;
import com.company.blog.search.service.SearchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/search/articles")
/**
 * 搜索索引的内部维护入口，仅由文章服务的 Outbox 投递器调用。
 */
public class InternalSearchController {
    private final SearchService searchService;
    private final String internalToken;

    public InternalSearchController(
            SearchService searchService,
            @Value("${blog.internal.search-token:local-search-token}") String internalToken
    ) {
        this.searchService = searchService;
        this.internalToken = internalToken;
    }

    @PostMapping("/index")
    /** 用文章发布事件中的快照建立或覆盖索引文档。 */
    public void index(@RequestHeader("X-Internal-Token") String token, @RequestBody IndexArticleRequest request) {
        requireToken(token);
        searchService.index(ArticleSearchDocument.from(request));
    }

    @DeleteMapping("/{articleId}")
    /** 删除文章对应的搜索文档，供后续下线或删除流程调用。 */
    public void delete(@RequestHeader("X-Internal-Token") String token, @PathVariable String articleId) {
        requireToken(token);
        searchService.delete(articleId);
    }

    private void requireToken(String token) {
        if (!internalToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal token");
        }
    }
}
