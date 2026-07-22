package com.company.blog.file.api;

import com.company.blog.file.FileMetadata;
import java.util.Optional;

public interface FileMetadataRepository {
    FileMetadata save(FileMetadata metadata);

    Optional<FileMetadata> findById(String fileId);

    void markUploadVerified(String fileId, String versionId);

    void bind(String fileId, String resourceType, String resourceId);
}
