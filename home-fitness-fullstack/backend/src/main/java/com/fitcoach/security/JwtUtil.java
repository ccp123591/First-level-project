package com.fitcoach.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expire-hours}")
    private long accessTokenExpireHours;

    @Value("${jwt.refresh-token-expire-days}")
    private long refreshTokenExpireDays;

    private final Environment env;

    public JwtUtil(Environment env) {
        this.env = env;
    }

    /** 启动时校验 secret：生产 profile 下 < 32 字节直接 fail-fast。 */
    @PostConstruct
    void validate() {
        boolean isProd = env != null && java.util.Arrays.asList(env.getActiveProfiles()).contains("prod");
        int len = secret == null ? 0 : secret.getBytes(StandardCharsets.UTF_8).length;
        if (isProd && len < 32) {
            throw new IllegalStateException(
                    "JWT secret 长度不足（要求 ≥ 32 字节）。请在生产环境通过 JWT_SECRET 环境变量配置强随机密钥。");
        }
        if (len < 32) {
            log.warn("[JWT] secret 长度 {} 字节 < 32（dev 容忍；prod 启动会被拒绝）", len);
        }
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String username, String role) {
        long now = System.currentTimeMillis();
        long expireMs = accessTokenExpireHours * 3600 * 1000;
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireMs))
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        long now = System.currentTimeMillis();
        long expireMs = refreshTokenExpireDays * 24 * 3600 * 1000;
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(java.util.UUID.randomUUID().toString())   // jti — 仅 refresh token 携带
                .claim("type", "refresh")
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireMs))
                .signWith(getKey())
                .compact();
    }

    /** 取 refresh token 的 jti（access token 没有 jti，会返回 null）。 */
    public String getJti(String token) {
        return parseToken(token).getId();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserIdFromToken(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    public boolean isTokenValid(String token) {
        try {
            Claims c = parseToken(token);
            return c.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
