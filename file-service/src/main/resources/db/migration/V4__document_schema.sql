COMMENT ON TABLE file_metadata IS 'MinIO 对象的元数据、归属与校验状态';
COMMENT ON COLUMN file_metadata.id IS '文件唯一标识';
COMMENT ON COLUMN file_metadata.owner_id IS '上传文件的用户标识';
COMMENT ON COLUMN file_metadata.object_key IS 'MinIO 中对象的唯一存储键';
COMMENT ON COLUMN file_metadata.original_name IS '用户上传时的原始文件名';
COMMENT ON COLUMN file_metadata.content_type IS '允许的文件 MIME 类型';
COMMENT ON COLUMN file_metadata.size_bytes IS '文件声明且已核验的字节大小';
COMMENT ON COLUMN file_metadata.created_at IS '上传凭证创建时间';
COMMENT ON COLUMN file_metadata.upload_verified IS '对象是否已完成大小、类型和内容校验';
COMMENT ON COLUMN file_metadata.verified_version_id IS '通过校验的 MinIO 对象版本标识';

COMMENT ON TABLE file_binding IS '文件与文章等业务资源的关联关系';
COMMENT ON COLUMN file_binding.file_id IS '关联文件标识';
COMMENT ON COLUMN file_binding.resource_type IS '业务资源类型，如 article';
COMMENT ON COLUMN file_binding.resource_id IS '业务资源唯一标识';
COMMENT ON COLUMN file_binding.created_at IS '关联创建时间';
