package com.fitcoach.auth;

import com.fitcoach.common.ApiResult;
import com.fitcoach.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "01. 认证", description = "登录 / 注册 / 验证码 / Token 刷新")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @Operation(summary = "邮箱密码登录")
    @PostMapping("/login/email")
    public ApiResult<Map<String, Object>> loginByEmail(@Valid @RequestBody LoginEmailRequest req) {
        return ApiResult.ok(authService.loginByEmail(req.getEmail(), req.getPassword()));
    }

    @Operation(summary = "手机验证码登录")
    @PostMapping("/login/phone")
    public ApiResult<Map<String, Object>> loginByPhone(@Valid @RequestBody LoginPhoneRequest req) {
        return ApiResult.ok(authService.loginByPhone(req.getPhone(), req.getCode()));
    }

    @Operation(summary = "游客登录（设备 ID）")
    @PostMapping("/login/guest")
    public ApiResult<Map<String, Object>> loginAsGuest(@RequestBody Map<String, String> body) {
        return ApiResult.ok(authService.loginAsGuest(body.get("deviceId")));
    }

    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms/send")
    public ApiResult<Void> sendSms(@Valid @RequestBody SendCodeRequest req) {
        authService.sendCode("sms", req.getTarget(), req.getPurpose());
        return ApiResult.ok(null, "已发送");
    }

    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/email/send")
    public ApiResult<Void> sendEmail(@Valid @RequestBody SendCodeRequest req) {
        authService.sendCode("email", req.getTarget(), req.getPurpose());
        return ApiResult.ok(null, "已发送");
    }

    @Operation(summary = "注册账号（邮箱）")
    @PostMapping("/register")
    public ApiResult<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResult.ok(authService.register(req.getEmail(), req.getPassword(), req.getNickname()));
    }

    @Operation(summary = "刷新 Access Token")
    @PostMapping("/refresh")
    public ApiResult<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        return ApiResult.ok(authService.refresh(body.get("refreshToken")));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        return ApiResult.ok(null, "已退出");
    }

    @Operation(summary = "获取当前用户")
    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me() {
        return ApiResult.ok(authService.currentUser(SecurityUtil.currentUserId()));
    }

    @Operation(summary = "找回密码（发送重置邮件）")
    @PostMapping("/password/forgot")
    public ApiResult<Void> passwordForgot(@Valid @RequestBody PasswordForgotRequest req) {
        passwordResetService.forgot(req.getEmail());
        return ApiResult.ok(null, "如果该邮箱已注册，将收到重置邮件");
    }

    @Operation(summary = "应用密码重置")
    @PostMapping("/password/reset")
    public ApiResult<Void> passwordReset(@Valid @RequestBody PasswordResetRequest req) {
        passwordResetService.reset(req.getToken(), req.getNewPassword());
        return ApiResult.ok(null, "密码已更新");
    }

    // ---- DTOs ----

    @Data
    public static class LoginEmailRequest {
        @NotBlank @Email private String email;
        @NotBlank @Size(min = 6) private String password;
    }

    @Data
    public static class LoginPhoneRequest {
        @NotBlank
        @Pattern(regexp = "1[3-9]\\d{9}", message = "手机号格式不正确")
        private String phone;
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "验证码必须为 6 位数字")
        private String code;
    }

    @Data
    public static class SendCodeRequest {
        @NotBlank private String target;          // phone 或 email
        @Pattern(regexp = "login|reset", message = "purpose 只能为 login 或 reset")
        private String purpose;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank @Email private String email;
        @NotBlank @Size(min = 8) private String password;
        private String nickname;
    }

    @Data
    public static class PasswordForgotRequest {
        @NotBlank @Email private String email;
    }

    @Data
    public static class PasswordResetRequest {
        @NotBlank private String token;
        @NotBlank @Size(min = 8) private String newPassword;
    }
}
