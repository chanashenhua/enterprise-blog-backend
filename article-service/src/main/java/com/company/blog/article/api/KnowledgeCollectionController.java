package com.company.blog.article.api;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/collections")
public class KnowledgeCollectionController {
    private final KnowledgeCollectionService service;

    public KnowledgeCollectionController(KnowledgeCollectionService service) {
        this.service = service;
    }

    @GetMapping
    public List<KnowledgeCollectionSummary> list(
            @RequestHeader HttpHeaders headers,
            @RequestParam(name = "mine", defaultValue = "false") boolean mine,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        return service.list(CallerContext.from(headers), mine, limit);
    }

    @GetMapping("/candidates")
    public List<HomeFeedItem> candidates(
            @RequestHeader HttpHeaders headers,
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        return service.candidateArticles(CallerContext.from(headers), limit);
    }

    @GetMapping("/{collectionId}")
    public KnowledgeCollectionDetailResponse get(
            @PathVariable("collectionId") String collectionId,
            @RequestHeader HttpHeaders headers
    ) {
        return service.get(collectionId, CallerContext.from(headers));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KnowledgeCollectionDetailResponse create(
            @RequestHeader HttpHeaders headers,
            @RequestBody SaveKnowledgeCollectionRequest request
    ) {
        return service.create(CallerContext.from(headers), request);
    }

    @PutMapping("/{collectionId}")
    public KnowledgeCollectionDetailResponse update(
            @PathVariable("collectionId") String collectionId,
            @RequestHeader HttpHeaders headers,
            @RequestBody SaveKnowledgeCollectionRequest request
    ) {
        return service.update(collectionId, CallerContext.from(headers), request);
    }

    @DeleteMapping("/{collectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable("collectionId") String collectionId,
            @RequestHeader HttpHeaders headers
    ) {
        service.delete(collectionId, CallerContext.from(headers));
    }
}
