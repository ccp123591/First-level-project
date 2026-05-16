package com.fitcoach.auth;

import com.fitcoach.exception.BusinessException;
import com.fitcoach.infra.notify.MailSender;
import com.fitcoach.infra.notify.SmsSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;

/**
 * 验证码生成 + 校验 — Redis 存 SHA-256 hash，TTL 5 分钟，60 秒冷却防刷。
 *
 * Key 设计：
 *   verify:&lt;channel&gt;:&lt;target&gt;       —— 哈希后的验证码，TTL 300s
 *   verify:sent:&lt;channel&gt;:&lt;target&gt;  —— 冷却标记，TTL 60s
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyCodeService {

    private static final Set<String> CHANNELS = Set.of("sms", "email");
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final Duration COOLDOWN = Duration.ofSeconds(60);
    private static final SecureRandom RNG = new SecureRandom();

    private final StringRedisTemplate redis;
    private final SmsSender smsSender;
    private final MailSender mailSender;

    /** 生成验证码并发送；返回明文（仅供单元测试断言，生产不要泄漏到响应）。 */
    public String generate(String channel, String target, String purpose) {
        validateChannel(channel);
        if (target == null || target.isBlank()) throw new BusinessException(400, "target 不能为空");

        String cooldownKey = cooldownKey(channel, target);
        if (Boolean.TRUE.equals(redis.hasKey(cooldownKey))) {
            throw new BusinessException(429, "发送过于频繁，请稍后再试");
        }

        String raw = sixDigit();
        String hashed = hash(raw, channel, target, purpose);
        redis.opsForValue().set(codeKey(channel, target), hashed, TTL);
        redis.opsForValue().set(cooldownKey, "1", COOLDOWN);

        if ("sms".equals(channel)) {
            smsSender.send(target, raw, purpose);
        } else {
            mailSender.sendCode(target, raw, purpose);
        }
        return raw;
    }

    /** 校验并消费（成功后删除 key，单次有效）。 */
    public boolean verify(String channel, String target, String purpose, String code) {
        if (code == null || code.isBlank()) return false;
        validateChannel(channel);
        String stored = redis.opsForValue().get(codeKey(channel, target));
        if (stored == null) return false;
        String expected = hash(code, channel, target, purpose);
        if (!stored.equals(expected)) return false;
        redis.delete(codeKey(channel, target));
        return true;
    }

    // ---- helpers ----

    private static void validateChannel(String channel) {
        if (!CHANNELS.contains(channel)) {
            throw new BusinessException(400, "channel 必须为 sms 或 email");
        }
    }

    private static String codeKey(String channel, String target) {
        return "verify:" + channel + ":" + target;
    }

    private static String cooldownKey(String channel, String target) {
        return "verify:sent:" + channel + ":" + target;
    }

    private static String sixDigit() {
        int n = RNG.nextInt(1_000_000);
        return String.format("%06d", n);
    }

    /** 公开静态以便测试断言。salt 用 target+purpose 避免不同上下文相同 code 共享 hash。 */
    public static String hash(String raw, String channel, String target, String purpose) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String payload = raw + ":" + channel + ":" + target + ":" + purpose;
            return HexFormat.of().formatHex(md.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
