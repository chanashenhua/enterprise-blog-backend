package com.company.blog.review.api;

import com.company.blog.review.ReviewTicket;

public record ReviewTicketResponse(String id, String articleId, String status) {
    static ReviewTicketResponse from(ReviewTicket ticket) {
        return new ReviewTicketResponse(ticket.id(), ticket.articleId(), ticket.status().name());
    }
}