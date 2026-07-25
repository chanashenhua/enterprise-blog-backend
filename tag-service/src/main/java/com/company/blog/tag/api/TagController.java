package com.company.blog.tag.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/tags")
/**
 * 供文章服务校验标签 ID 的内部接口。
 *
 * <p>调用方得到有效和未知标签列表；已停用标签按未知标签处理，避免新文章继续引用。</p>
 */
public class TagController {
    private final CatalogService catalogService;

    public TagController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/validate")
    /** 批量校验标签 ID，并保留请求中的顺序以方便前端定位输入项。 */
    public TagValidationResponse validate(@RequestParam(name = "ids", defaultValue = "") List<String> ids) {
        return catalogService.validateTags(ids);
    }

}
