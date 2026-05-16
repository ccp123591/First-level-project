package com.fitcoach.infra.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储抽象 — save 返回相对 URL（如 /uploads/avatar/u42-abc123.png）。
 * 生产可换接 S3 / 阿里 OSS 实现。
 */
public interface StorageService {
    String save(String category, Long userId, MultipartFile file, String sanitizedExt);
}
