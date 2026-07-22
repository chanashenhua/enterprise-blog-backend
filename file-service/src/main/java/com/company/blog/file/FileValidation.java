package com.company.blog.file;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 上传请求的第一道静态校验。
 *
 * <p>它校验文件名扩展名、客户端声明 MIME 类型和大小限制；实际字节内容由
 * {@code UploadedContentValidator} 在对象上传后复核。</p>
 */
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
        // MIME 类型和扩展名必须形成允许的组合，不能仅凭客户端传来的 Content-Type 放行。
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
