package com.company.blog.review.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class ReviewExceptionHandler {
    @ExceptionHandler(ReviewTicketNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse notFound(ReviewTicketNotFoundException ex) {
        return new ErrorResponse("REVIEW_TICKET_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ReviewTicketStateConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse conflict(ReviewTicketStateConflictException ex) {
        return new ErrorResponse("REVIEW_TICKET_STATE_CONFLICT", ex.getMessage());
    }

    @ExceptionHandler({IllegalStateException.class, RestClientException.class})
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResponse callbackFailed(RuntimeException ex) {
        return new ErrorResponse("ARTICLE_CALLBACK_FAILED", ex.getMessage());
    }

    record ErrorResponse(String code, String message) {
    }
}