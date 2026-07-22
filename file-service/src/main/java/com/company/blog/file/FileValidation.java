package com.company.blog.file;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class FileValidation {
    private static final Map<String, Set<String>> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/png", Set.of("png"),
            "image/jpeg", Set.of("jpg", "jpeg"),
            "application/pdf", Set.of("pdf")
    );

    private final Set<String> allowedContentTypes;
    private final long maxSizeBytes;

    public FileValidation(Set<String> allowedContentTypes, long maxSizeBytes) {
        this.allowedContentTypes = (allowedContentTypes == null ? Set.<String>of() : allowedContentTypes).stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (maxSizeBytes <= 0) {
            throw new IllegalArgumentException("maxSizeBytes must be positive");
        }
        this.maxSizeBytes = maxSizeBytes;
    }

    public FileValidationResult validate(String originalName, String contentType, long sizeBytes) {
        if (originalName == null || originalName.isBlank()) {
            return FileValidationResult.rejected("FILE_NAME_INVALID");
        }
        if (sizeBytes <= 0) {
            return FileValidationResult.rejected("FILE_SIZE_INVALID");
        }
        if (sizeBytes > maxSizeBytes) {
            return FileValidationResult.rejected("FILE_SIZE_LIMIT_EXCEEDED");
        }

        String normalizedContentType = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        Set<String> allowedExtensions = EXTENSIONS_BY_CONTENT_TYPE.get(normalizedContentType);
        if (!allowedContentTypes.contains(normalizedContentType) || allowedExtensions == null
                || !allowedExtensions.contains(extensionOf(originalName))) {
            return FileValidationResult.rejected("FILE_TYPE_NOT_ALLOWED");
        }
        return FileValidationResult.accepted();
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 1 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
