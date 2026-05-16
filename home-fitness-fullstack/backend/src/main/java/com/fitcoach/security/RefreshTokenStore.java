package com.fitcoach.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;

/**
 * Refresh token 黑名单 — 仅 Redis 存储，按剩余过期时间设置 TTL。
 * Redis 不可用时 isRevoked 返回 false（fail-open）— 保证 dev 无 Redis 时业务正常；
 * 真正的吊销在生产 Redis 必备的前提下才严格生效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String PREFIX = "rt:black:";

    private final StringRedisTemplate redis;

    public void revoke(String jti, Long userId, Date exp, String reason) {
        if (jti == null || jti.isBlank()) return;
        long now = System.currentTimeMillis();
        long remainingMs = exp == null ? 0 : exp.getTime() - now;
        if (remainingMs <= 0) return;                 // 已过期，不必占用 Redis
        try {
            redis.opsForValue().set(PREFIX + jti, String.valueOf(userId),
                    Duration.ofMillis(remainingMs));
            log.info("[refresh-blacklist] revoked jti={} user={} reason={}", jti, userId, reason);
        } catch (Exception e) {
            log.warn("[refresh-blacklist] Redis 不可用，跳过写入 jti={}", jti);
        }
    }

    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) return false;
        try {
            return Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
        } catch (Exception e) {
            log.warn("[refresh-blacklist] Redis 查询失败，fail-open: {}", e.getMessage());
            return false;
        }
    }
}
