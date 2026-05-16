package com.fitcoach.auth;

import com.fitcoach.exception.BusinessException;
import com.fitcoach.infra.notify.MailSender;
import com.fitcoach.user.User;
import com.fitcoach.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 邮箱找回密码 — 生成 token 存 Redis（sha256 后存），通过邮件发出重置链接。
 * 重置时校验 token 并应用 PasswordPolicy。Token 单次有效，TTL 30 分钟。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redis;
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final MailSender mailSender;

    @Value("${app.password-reset.front-base:http://localhost:5173}")
    private String frontBase;

    /** 触发找回：邮件中带 link?token=&lt;raw&gt;。返回值仅用于测试断言；生产不要泄漏。 */
    @Transactional
    public String forgot(String email) {
        if (email == null || email.isBlank()) throw new BusinessException(400, "email 不能为空");
        // 不暴露 "用户不存在" — 避免账号枚举
        User u = userRepo.findByEmail(email.trim()).orElse(null);
        if (u == null) {
            log.info("[reset] forgot 请求邮箱未注册（静默成功）: {}", email);
            return null;
        }
        String raw = UUID.randomUUID().toString().replace("-", "");
        String key = "pwreset:" + sha256(raw);
        redis.opsForValue().set(key, String.valueOf(u.getId()), TTL);
        String link = frontBase + "/reset?token=" + raw;
        mailSender.send(email, "FitCoach 密码重置",
                "您正在重置 FitCoach 密码：" + link + "\n该链接 30 分钟内有效，单次有效。");
        log.info("[reset] 已为 userId={} 发出重置邮件", u.getId());
        return raw;
    }

    /** 应用重置：token 反查 userId，校验密码强度，bcrypt 写回，删 token。 */
    @Transactional
    public void reset(String token, String newPassword) {
        if (token == null || token.isBlank()) throw new BusinessException(400, "token 无效");
        PasswordPolicy.validate(newPassword);
        String key = "pwreset:" + sha256(token);
        String uid = redis.opsForValue().get(key);
        if (uid == null) throw new BusinessException(400, "重置链接无效或已过期");
        User u = userRepo.findById(Long.valueOf(uid))
                .orElseThrow(() -> new BusinessException(400, "用户不存在"));
        u.setPasswordHash(encoder.encode(newPassword));
        userRepo.save(u);
        redis.delete(key);
        log.info("[reset] userId={} 已成功重置密码", u.getId());
    }

    static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
