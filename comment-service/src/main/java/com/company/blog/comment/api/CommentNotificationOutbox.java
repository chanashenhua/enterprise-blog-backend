package com.company.blog.comment.api;

import com.company.blog.comment.Comment;

public interface CommentNotificationOutbox {
    void appendReplyNotification(Comment reply, Comment parent);
}
