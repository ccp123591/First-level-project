package com.fitcoach.infra.storage;

import com.fitcoach.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 本地磁盘存储 — 写入到 ${upload.dir}/<category>/u<userId>-<rand>.<ext>。
 * 完全不信任客户端文件名，自己拼接最终路径。
 */
@Slf4j
@Component
public class LocalStorageService implements StorageService {

    private static final SecureRandom RNG = new SecureRandom();

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public String save(String category, Long userId, MultipartFile file, String sanitizedExt) {
        if (category == null || category.isBlank()) category = "misc";
        try {
            Path dir = Paths.get(uploadDir, category).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            byte[] rand = new byte[6];
            RNG.nextBytes(rand);
            String fileName = "u" + userId + "-" + HexFormat.of().formatHex(rand) + "." + sanitizedExt;
            Path target = dir.resolve(fileName);
            file.transferTo(target.toFile());
            String publicUrl = "/uploads/" + category + "/" + fileName;
            log.info("[storage] saved {} ({} bytes) → {}", publicUrl, file.getSize(), target);
            return publicUrl;
        } catch (IOException e) {
            log.error("[storage] write failed", e);
            throw new BusinessException(500, "文件保存失败");
        }
    }
}
