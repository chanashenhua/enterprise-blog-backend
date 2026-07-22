package com.company.blog.file.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
/**
 * 文件上传、下载和业务绑定的接口入口。
 *
 * <p>文件二进制不经过本服务转发：客户端直接使用 MinIO 预签名地址上传或下载；本服务只保存元数据、
 * 控制归属，并在绑定到文章前确认对象确实通过校验。</p>
 */
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
    /** 为当前用户创建一次受类型和大小限制的直传 MinIO 表单。 */
    public CreateUploadUrlResponse createUploadUrl(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody CreateUploadUrlRequest request
    ) {
        return fileService.createUploadUrl(userId, request);
    }

    @GetMapping("/api/files/{fileId}/download-url")
    /** 为文件所有者签发短时下载地址，签发前会核验实际上传内容。 */
    public CreateDownloadUrlResponse createDownloadUrl(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("fileId") String fileId
    ) {
        return fileService.createDownloadUrl(userId, fileId);
    }

    @PostMapping("/internal/files/bind")
    /** 供文章等内部服务把已验证文件关联到业务资源。 */
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
