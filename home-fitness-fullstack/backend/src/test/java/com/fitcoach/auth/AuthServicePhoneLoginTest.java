package com.fitcoach.auth;

import com.fitcoach.exception.BusinessException;
import com.fitcoach.security.JwtUtil;
import com.fitcoach.user.User;
import com.fitcoach.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServicePhoneLoginTest {

    @Mock UserRepository userRepo;
    @Mock PasswordEncoder encoder;
    @Mock JwtUtil jwtUtil;
    @Mock VerifyCodeService verifyCode;

    @InjectMocks AuthService service;

    @Test
    void loginByPhone_creates_user_when_first_time_and_code_valid() {
        String phone = "13900000000";
        given(verifyCode.verify("sms", phone, "login", "123456")).willReturn(true);
        given(userRepo.findByPhone(phone)).willReturn(Optional.empty());
        given(encoder.encode(any())).willReturn("hashed");
        given(userRepo.save(any())).willAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(101L);
            return u;
        });
        given(jwtUtil.generateAccessToken(eq(101L), any(), eq("USER"))).willReturn("access");
        given(jwtUtil.generateRefreshToken(101L)).willReturn("refresh");

        Map<String, Object> resp = service.loginByPhone(phone, "123456");

        assertThat(resp).containsKeys("accessToken", "refreshToken", "user");
        verify(userRepo).save(any(User.class));
    }

    @Test
    void loginByPhone_uses_existing_user_when_phone_known() {
        String phone = "13900000000";
        User existing = User.builder()
                .id(7L).phone(phone).nickname("Tom")
                .role("USER").status("ACTIVE").passwordHash("x").build();
        given(verifyCode.verify("sms", phone, "login", "123456")).willReturn(true);
        given(userRepo.findByPhone(phone)).willReturn(Optional.of(existing));
        given(jwtUtil.generateAccessToken(eq(7L), any(), eq("USER"))).willReturn("access");
        given(jwtUtil.generateRefreshToken(7L)).willReturn("refresh");

        Map<String, Object> resp = service.loginByPhone(phone, "123456");

        assertThat(resp).containsKey("accessToken");
        verify(userRepo, org.mockito.Mockito.never()).save(any(User.class));
    }

    @Test
    void loginByPhone_rejects_invalid_code_with_400() {
        given(verifyCode.verify(any(), any(), any(), any())).willReturn(false);
        assertThatThrownBy(() -> service.loginByPhone("13900000000", "000000"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码无效");
    }

    @Test
    void sendCode_delegates_to_verify_service() {
        service.sendCode("sms", "13900000000", "login");
        verify(verifyCode).generate("sms", "13900000000", "login");
    }
}
