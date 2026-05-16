package com.fitcoach.infra.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis token-bucket 限流 — 通过 Lua 保证 INCR+EXPIRE 的原子性。
 *
 * 实现：每个 (scope,id) 维护一个 Redis 计数器；首次访问 SET 1 + EXPIRE windowSeconds；
 * 后续 INCR；超过 capacity 返回 false。
 *
 * Redis 不可用时 fail-open（返回 true），避免基础设施抖动导致全站不可用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private static final String LUA = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
            end
            if current > tonumber(ARGV[1]) then
              return 0
            else
              return 1
            end
            """;

    private static final RedisScript<Long> SCRIPT = new DefaultRedisScript<>(LUA, Long.class);

    private final StringRedisTemplate redis;

    /**
     * @param scope          逻辑分组（auth.login / api.global …）
     * @param id             身份（ip / userId / email）
     * @param capacity       窗口内最多 N 次
     * @param windowSeconds  窗口大小（秒）
     * @return true 通过；false 超限
     */
    public boolean tryAcquire(String scope, String id, int capacity, int windowSeconds) {
        if (id == null || id.isBlank()) return true;
        String key = "rl:" + scope + ":" + id;
        try {
            Long ok = redis.execute(SCRIPT, List.of(key),
                    String.valueOf(capacity), String.valueOf(windowSeconds));
            return ok == null || ok == 1L;
        } catch (Exception e) {
            log.warn("[rate-limit] Redis 不可用，fail-open: scope={} id={} msg={}", scope, id, e.getMessage());
            return true;
        }
    }
}
