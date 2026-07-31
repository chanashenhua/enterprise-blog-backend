package com.company.blog.notification.api;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationGovernanceService {
    private static final int MAX_LIMIT = 200;

    private final AdminNotificationRepository repository;

    public NotificationGovernanceService(AdminNotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public NotificationGovernanceOverview overview() {
        return repository.overview();
    }

    @Transactional(readOnly = true)
    public List<AdminNotificationRecord> search(
            String recipientUserId,
            String type,
            String state,
            int requestedLimit
    ) {
        int limit = Math.max(1, Math.min(requestedLimit, MAX_LIMIT));
        return repository.search(new NotificationGovernanceQuery(
                normalize(recipientUserId),
                normalize(type),
                readState(state),
                limit
        ));
    }

    private static Boolean readState(String state) {
        if (state == null || state.isBlank() || "ALL".equalsIgnoreCase(state)) {
            return null;
        }
        if ("READ".equalsIgnoreCase(state)) {
            return true;
        }
        if ("UNREAD".equalsIgnoreCase(state)) {
            return false;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "State must be ALL, READ, or UNREAD");
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
