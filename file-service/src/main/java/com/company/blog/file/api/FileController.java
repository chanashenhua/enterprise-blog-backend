package com.company.blog.file.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileController {
    private final FileService fileService;
    private final String internalToken;

    public FileController(
            FileService fileService,
            @Value("${blog.internal.file-token:local-file-token}") String internalToken
    ) {
        this.fileService = fileService;
        this.internalToken = internalToken;
    }

    @PostMapping("/api/files/upload-url")
    public CreateUploadUrlResponse createUploadUrl(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody CreateUploadUrlRequest request
    ) {
        return fileService.createUploadUrl(userId, request);
    }

    @GetMapping("/api/files/{fileId}/download-url")
    public CreateDownloadUrlResponse createDownloadUrl(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("fileId") String fileId
    ) {
        return fileService.createDownloadUrl(userId, fileId);
    }

    @PostMapping("/internal/files/bind")
    public BindFilesResponse bind(
            @RequestHeader("X-Internal-Token") String token,
            @RequestBody BindFilesRequest request
    ) {
        if (!internalToken.equals(token)) {
            throw new FileAccessDeniedException("internal");
        }
        return fileService.bind(request);
    }
}
