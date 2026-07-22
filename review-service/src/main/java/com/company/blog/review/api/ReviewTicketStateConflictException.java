package com.company.blog.review.api;

public class ReviewTicketStateConflictException extends RuntimeException {
    public ReviewTicketStateConflictException(String ticketId) {
        super("Review ticket cannot transition from its current state: " + ticketId);
    }
}
