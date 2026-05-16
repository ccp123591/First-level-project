package com.fitcoach.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.common.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 安全过滤链触发未认证/无权限时，返回统一 JSON 而非默认空体。
 */
@Component
public class RestAuthEntryPoints {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Component("restAuthenticationEntryPoint")
    public static class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response,
                             AuthenticationException e) throws IOException {
            write(response, HttpStatus.UNAUTHORIZED, 401, "未登录或登录已过期");
        }
    }

    @Component("restAccessDeniedHandler")
    public static class RestAccessDeniedHandler implements AccessDeniedHandler {
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                           AccessDeniedException e) throws IOException {
            write(response, HttpStatus.FORBIDDEN, 403, "无权访问");
        }
    }

    private static void write(HttpServletResponse response, HttpStatus status, int code, String msg) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResult<Void> body = ApiResult.fail(code, msg);
        response.getWriter().write(MAPPER.writeValueAsString(body));
    }
}
