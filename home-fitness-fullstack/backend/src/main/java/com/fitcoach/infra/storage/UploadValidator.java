package com.fitcoach.infra.storage;

import com.fitcoach.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * 上传安全校验：扩展名白名单 + Tika 真实 MIME 检测 + 大小上限。
 * 调用方拿到 sanitized extension（小写），用于落盘时拼接 — 不信任客户端文件名。
 */
@Slf4j
@Component
public class UploadValidator {

    private static final Set<String> IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> IMAGE_MIMES = Set.of("image/jpeg", "image/png", "image/webp");

    @Value("${upload.max-image-size-bytes:5242880}")  // 5 MB default
    private long maxImageBytes;

    /**
     * 校验并返回安全的扩展名（小写、不带点）。
     */
    public String validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "未上传文件");
        }
        if (file.getSize() > maxImageBytes) {
            throw new BusinessException(413, "文件过大（限 " + (maxImageBytes / 1024) + " KB）");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            throw new BusinessException(400, "缺少文件扩展名");
        }
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!IMAGE_EXTS.contains(ext)) {
            throw new BusinessException(400, "不支持的扩展名：" + ext);
        }

        // Tika 真实 MIME 嗅探（前 N 字节）—— 防止把 EXE 改名为 PNG 上传
        String detected = detectMime(file, name);
        if (!IMAGE_MIMES.contains(detected)) {
            log.warn("[upload] MIME 嗅探失败：name={} detected={}", name, detected);
            throw new BusinessException(400, "文件内容与扩展名不匹配");
        }
        // 标准化 jpeg → jpg
        return "jpeg".equals(ext) ? "jpg" : ext;
    }

    private static String detectMime(MultipartFile file, String name) {
        try (InputStream is = new BufferedInputStream(file.getInputStream())) {
            Metadata md = new Metadata();
            md.set(TikaCoreProperties.RESOURCE_NAME_KEY, name);
            MediaType mt = new DefaultDetector().detect(is, md);
            return mt == null ? "application/octet-stream" : mt.toString();
        } catch (IOException e) {
            throw new BusinessException(400, "文件读取失败");
        }
    }
}
