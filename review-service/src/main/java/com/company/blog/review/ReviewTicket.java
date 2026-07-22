package com.company.blog.review;

import java.util.Set;

/**
 * 一次文章发布请求对应的审核票据。
 *
 * <p>{@code reviewRequestId} 用于区分同一文章的多次提交，也是文章服务识别迟到审核回调的依据。</p>
 */
public record ReviewTicket(
        String id,
        String articleId,
        String reviewRequestId,
        String authorId,
        String visibilityType,
        Set<String> targetOrgIds,
        ReviewTicketStatus status
) {
    public ReviewTicket {
        targetOrgIds = targetOrgIds == null ? Set.of() : Set.copyOf(targetOrgIds);
    }
}
