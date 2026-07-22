package com.company.blog.tag.api;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/tags")
/**
 * 供文章服务校验标签 ID 的内部接口。
 *
 * <p>目前标签目录为演示数据；调用方得到有效和未知标签列表，能够在保存草稿前把错误准确反馈给作者。</p>
 */
public class TagController {
    private static final Set<String> KNOWN_TAG_IDS = Set.of(
            "java",
            "spring-cloud",
            "redis",
            "postgresql",
            "elasticsearch"
    );

    @GetMapping("/validate")
    /** 批量校验标签 ID，并保留请求中的顺序以方便前端定位输入项。 */
    public TagValidationResponse validate(@RequestParam(name = "ids", defaultValue = "") List<String> ids) {
        LinkedHashSet<String> validIds = new LinkedHashSet<>();
        LinkedHashSet<String> unknownIds = new LinkedHashSet<>();
        for (String id : ids) {
            if (KNOWN_TAG_IDS.contains(id)) {
                validIds.add(id);
            } else if (id != null && !id.isBlank()) {
                unknownIds.add(id);
            }
        }
        return new TagValidationResponse(unknownIds.isEmpty(), List.copyOf(validIds), List.copyOf(unknownIds));
    }

}
