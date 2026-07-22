package com.company.blog.review.api;

public class ReviewTicketNotFoundException extends RuntimeException {
    public ReviewTicketNotFoundException(String ticketId) {
        super("Review ticket not found: " + ticketId);
    }
}