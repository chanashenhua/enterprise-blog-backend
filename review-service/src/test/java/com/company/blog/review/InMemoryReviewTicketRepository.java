package com.company.blog.review;

import com.company.blog.review.api.ReviewTicketNotFoundException;
import com.company.blog.review.api.ReviewTicketRepository;
import com.company.blog.review.api.ReviewTicketStateConflictException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class InMemoryReviewTicketRepository implements ReviewTicketRepository {
    private final ConcurrentMap<String, ReviewTicket> tickets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> ticketIdsByReviewRequest = new ConcurrentHashMap<>();

    @Override
    public synchronized ReviewTicket save(ReviewTicket ticket) {
        return findOrCreateForArticle(ticket);
    }

    @Override
    public synchronized ReviewTicket findOrCreateForArticle(ReviewTicket ticket) {
        String requestKey = requestKey(ticket.articleId(), ticket.reviewRequestId());
        String existingId = ticketIdsByReviewRequest.get(requestKey);
        if (existingId != null) {
            return tickets.get(existingId);
        }
        tickets.put(ticket.id(), ticket);
        ticketIdsByReviewRequest.put(requestKey, ticket.id());
        return ticket;
    }

    @Override
    public Optional<ReviewTicket> findById(String id) {
        return Optional.ofNullable(tickets.get(id));
    }

    @Override
    public synchronized ReviewTicket findPendingForTransition(String id) {
        ReviewTicket current = tickets.get(id);
        if (current == null) {
            throw new ReviewTicketNotFoundException(id);
        }
        if (current.status() != ReviewTicketStatus.PENDING) {
            throw new ReviewTicketStateConflictException(id);
        }
        return current;
    }

    @Override
    public synchronized ReviewTicket beginApproval(String id) {
        ReviewTicket current = requiredTicket(id);
        if (current.status() == ReviewTicketStatus.APPROVING) {
            return current;
        }
        if (current.status() != ReviewTicketStatus.PENDING) {
            throw new ReviewTicketStateConflictException(id);
        }
        return replaceStatus(current, ReviewTicketStatus.APPROVING);
    }

    @Override
    public synchronized ReviewTicket completeApproval(String id) {
        ReviewTicket current = requiredTicket(id);
        if (current.status() == ReviewTicketStatus.APPROVED) {
            return current;
        }
        if (current.status() != ReviewTicketStatus.APPROVING) {
            throw new ReviewTicketStateConflictException(id);
        }
        return replaceStatus(current, ReviewTicketStatus.APPROVED);
    }

    @Override
    public synchronized ReviewTicket beginRejection(String id) {
        ReviewTicket current = requiredTicket(id);
        if (current.status() == ReviewTicketStatus.REJECTING) {
            return current;
        }
        if (current.status() != ReviewTicketStatus.PENDING) {
            throw new ReviewTicketStateConflictException(id);
        }
        return replaceStatus(current, ReviewTicketStatus.REJECTING);
    }

    @Override
    public synchronized ReviewTicket completeRejection(String id) {
        ReviewTicket current = requiredTicket(id);
        if (current.status() == ReviewTicketStatus.REJECTED) {
            return current;
        }
        if (current.status() != ReviewTicketStatus.REJECTING) {
            throw new ReviewTicketStateConflictException(id);
        }
        return replaceStatus(current, ReviewTicketStatus.REJECTED);
    }

    @Override
    public synchronized ReviewTicket updateStatus(String id, ReviewTicketStatus status) {
        ReviewTicket current = requiredTicket(id);
        if (current.status() != ReviewTicketStatus.PENDING) {
            throw new ReviewTicketStateConflictException(id);
        }
        return replaceStatus(current, status);
    }

    private ReviewTicket requiredTicket(String id) {
        ReviewTicket current = tickets.get(id);
        if (current == null) {
            throw new ReviewTicketNotFoundException(id);
        }
        return current;
    }

    private ReviewTicket replaceStatus(ReviewTicket current, ReviewTicketStatus status) {
        ReviewTicket updated = new ReviewTicket(
                current.id(),
                current.articleId(),
                current.reviewRequestId(),
                current.authorId(),
                current.visibilityType(),
                current.targetOrgIds(),
                status
        );
        tickets.put(current.id(), updated);
        return updated;
    }

    private static String requestKey(String articleId, String reviewRequestId) {
        return articleId + ":" + reviewRequestId;
    }
}
