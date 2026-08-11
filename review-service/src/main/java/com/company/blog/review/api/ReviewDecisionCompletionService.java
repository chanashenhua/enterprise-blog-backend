package com.company.blog.review.api;

import com.company.blog.review.ReviewTicket;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewDecisionCompletionService {
    private final ReviewTicketRepository ticketRepository;
    private final ReviewAuditOutbox auditOutbox;

    public ReviewDecisionCompletionService(
            ReviewTicketRepository ticketRepository,
            ReviewAuditOutbox auditOutbox
    ) {
        this.ticketRepository = ticketRepository;
        this.auditOutbox = auditOutbox;
    }

    @Transactional
    public ReviewTicket completeApproval(String ticketId, HttpHeaders headers) {
        ReviewTicket ticket = ticketRepository.completeApproval(ticketId);
        auditOutbox.append(actorId(headers), roles(headers), "REVIEW_APPROVE", ticket, null);
        return ticket;
    }

    @Transactional
    public ReviewTicket completeRejection(String ticketId, HttpHeaders headers, String comment) {
        ReviewTicket ticket = ticketRepository.completeRejection(ticketId);
        String details = comment == null || comment.isBlank() ? null : "comment=" + comment.trim();
        auditOutbox.append(actorId(headers), roles(headers), "REVIEW_REJECT", ticket, details);
        return ticket;
    }

    private static String actorId(HttpHeaders headers) {
        return headers.getFirst("X-User-Id");
    }

    private static List<String> roles(HttpHeaders headers) {
        String value = headers.getFirst("X-User-Roles");
        return value == null || value.isBlank()
                ? List.of()
                : Arrays.stream(value.split(",")).map(String::trim).filter(role -> !role.isBlank()).distinct().toList();
    }
}
