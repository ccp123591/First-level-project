package com.fitcoach.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录失败计数 + 账号锁定（Redis 实现，对 Redis 异常 fail-open）。
 *
 * Key 设计：login:fail:&lt;identifier&gt; — 计数（TTL 15 分钟，每次失败重置 TTL）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final String PREFIX = "login:fail:";

    @Value("${security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.login.lock-window-minutes:15}")
    private int lockWindowMinutes;

    private final StringRedisTemplate redis;

    public void recordFailure(String identifier) {
        if (identifier == null || identifier.isBlank()) return;
        String key = PREFIX + identifier;
        try {
            Long count = redis.opsForValue().increment(key);
            redis.expire(key, Duration.ofMinutes(lockWindowMinutes));
            if (count != null && count >= maxAttempts) {
                log.warn("[lockout] identifier={} 达到失败上限 {}，账号将被锁定 {}min", identifier, count, lockWindowMinutes);
            }
        } catch (Exception e) {
            log.warn("[lockout] Redis 不可用，跳过失败计数: {}", e.getMessage());
        }
    }

    public boolean isLocked(String identifier) {
        if (identifier == null || identifier.isBlank()) return false;
        try {
            String v = redis.opsForValue().get(PREFIX + identifier);
            if (v == null) return false;
            try {
                return Long.parseLong(v) >= maxAttempts;
            } catch (NumberFormatException e) {
                return false;
            }
        } catch (Exception e) {
            log.warn("[lockout] Redis 查询失败，fail-open: {}", e.getMessage());
            return false;
        }
    }

    public void clear(String identifier) {
        if (identifier == null || identifier.isBlank()) return;
        try {
            redis.delete(PREFIX + identifier);
        } catch (Exception ignored) {
        }
    }

    int getMaxAttempts() { return maxAttempts; }
    int getLockWindowMinutes() { return lockWindowMinutes; }
}
