package com.fitcoach.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求级日志 + MDC 上下文：
 *   - X-Request-Id：取自请求头或新生成（8 位 hex）
 *   - MDC: requestId, userId
 *   - 完成后打印 [rid] METHOD PATH status=... duration=...ms user=...
 *   - 跳过 actuator / swagger / 静态 / 健康检查路径以避免噪声
 */
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Request-Id";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null
                || path.startsWith("/actuator/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/uploads/")
                || path.startsWith("/h2-console");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String rid = request.getHeader(HEADER);
        if (!StringUtils.hasText(rid)) rid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        response.setHeader(HEADER, rid);
        MDC.put("requestId", rid);

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = System.currentTimeMillis() - start;
            Long uid = currentUserIdSafe();
            MDC.put("userId", uid == null ? "anon" : String.valueOf(uid));
            log.info("[{}] {} {} status={} duration={}ms user={}",
                    rid, request.getMethod(), request.getRequestURI(),
                    response.getStatus(), ms, uid == null ? "anon" : uid);
            MDC.clear();
        }
    }

    private static Long currentUserIdSafe() {
        try {
            return SecurityUtil.currentUserIdOrNull();
        } catch (Exception e) {
            return null;
        }
    }
}
