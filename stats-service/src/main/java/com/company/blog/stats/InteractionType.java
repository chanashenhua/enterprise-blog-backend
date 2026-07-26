package com.company.blog.stats;

/**
 * 员工对文章产生的互动类型。
 *
 * <p>浏览记录只增加不撤销，点赞和收藏允许用户主动取消。</p>
 */
public enum InteractionType {
    VIEW,
    LIKE,
    FAVORITE
}
