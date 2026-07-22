package com.company.blog.file.api;

import com.company.blog.file.FileMetadata;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
/**
 * 文件元数据与业务绑定关系的 PostgreSQL 实现。
 *
 * <p>二进制对象仍保存在 MinIO；数据库只保存归属、校验结果和引用关系，以便授权查询和审计。</p>
 */
public class JdbcFileMetadataRepository implements FileMetadataRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcFileMetadataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public FileMetadata save(FileMetadata metadata) {
        jdbcTemplate.update(
                """
                        insert into file_metadata
                            (id, owner_id, object_key, original_name, content_type, size_bytes, created_at,
                             upload_verified, verified_version_id)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                metadata.id(),
                metadata.ownerId(),
                metadata.objectKey(),
                metadata.originalName(),
                metadata.contentType(),
                metadata.sizeBytes(),
                Timestamp.from(metadata.createdAt()),
                metadata.uploadVerified(),
                metadata.verifiedVersionId()
        );
        return metadata;
    }

    @Override
    public Optional<FileMetadata> findById(String fileId) {
        return jdbcTemplate.query(
                """
                        select id, owner_id, object_key, original_name, content_type, size_bytes, created_at,
                               upload_verified, verified_version_id
                        from file_metadata
                        where id = ?
                        """,
                (rs, rowNum) -> new FileMetadata(
                        rs.getString("id"),
                        rs.getString("owner_id"),
                        rs.getString("object_key"),
                        rs.getString("original_name"),
                        rs.getString("content_type"),
                        rs.getLong("size_bytes"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getBoolean("upload_verified"),
                        rs.getString("verified_version_id")
                ),
                fileId
        ).stream().findFirst();
    }

    @Override
    public void markUploadVerified(String fileId, String versionId) {
        jdbcTemplate.update(
                """
                        update file_metadata
                        set upload_verified = true, verified_version_id = ?
                        where id = ? and upload_verified = false
                        """,
                versionId,
                fileId
        );
    }

    @Override
    public void bind(String fileId, String resourceType, String resourceId) {
        jdbcTemplate.update(
                """
                        insert into file_binding (file_id, resource_type, resource_id)
                        values (?, ?, ?)
                on conflict do nothing
                        """,
                fileId,
                resourceType,
                resourceId
        );
    }
}
