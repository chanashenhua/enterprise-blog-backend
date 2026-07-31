package com.company.blog.audit.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/audits")
public class InternalAuditController {
    private final AuditApplicationService service;
    private final String internalToken;

    public InternalAuditController(
            AuditApplicationService service,
            @Value("${blog.internal.audit-token}") String internalToken
    ) {
        this.service = service;
        this.internalToken = internalToken;
    }

    @PostMapping
    public AuditRecordResponse create(
            @RequestHeader("X-Internal-Token") String token,
            @RequestBody CreateAuditRecordRequest request
    ) {
        if (!internalToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal token");
        }
        return service.create(request);
    }
}
