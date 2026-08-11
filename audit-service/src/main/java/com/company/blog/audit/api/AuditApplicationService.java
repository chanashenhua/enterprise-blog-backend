package com.company.blog.audit.api;

import com.company.blog.audit.AuditRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuditApplicationService {
    private static final int MAX_LIMIT = 200;

    private final AuditRepository repository;

    public AuditApplicationService(AuditRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AuditRecordResponse create(CreateAuditRecordRequest request) {
        requireRequest(request);
        Instant now = Instant.now();
        AuditRecord record = new AuditRecord(
                UUID.randomUUID().toString(),
                required(request.eventId(), "Event id is required"),
                required(request.sourceService(), "Source service is required"),
                required(request.actorId(), "Actor id is required"),
                normalizeRoles(request.actorRoles()),
                required(request.action(), "Action is required"),
                required(request.resourceType(), "Resource type is required"),
                required(request.resourceId(), "Resource id is required"),
                normalize(request.outcome(), "SUCCESS"),
                normalize(request.details(), null),
                normalize(request.traceId(), null),
                request.occurredAt() == null ? now : request.occurredAt(),
                now
        );
        return AuditRecordResponse.from(repository.save(record));
    }

    @Transactional(readOnly = true)
    public List<AuditRecordResponse> search(AuditQuery query) {
        if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time must not be after end time");
        }
        int limit = Math.max(1, Math.min(query.limit(), MAX_LIMIT));
        AuditQuery normalized = new AuditQuery(
                normalize(query.actorId(), null), normalize(query.action(), null),
                normalize(query.resourceType(), null), normalize(query.resourceId(), null),
                query.from(), query.to(), limit
        );
        return repository.search(normalized).stream().map(AuditRecordResponse::from).toList();
    }

    private static void requireRequest(CreateAuditRecordRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audit request is required");
        }
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static List<String> normalizeRoles(List<String> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream().filter(role -> role != null && !role.isBlank()).map(String::trim).distinct().toList();
    }
}
