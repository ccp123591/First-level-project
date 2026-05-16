package com.fitcoach.auth;

import com.fitcoach.exception.BusinessException;
import com.fitcoach.security.JwtUtil;
import com.fitcoach.security.LoginAttemptService;
import com.fitcoach.security.RefreshTokenStore;
import com.fitcoach.user.User;
import com.fitcoach.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceLockoutTest {

    @Mock UserRepository userRepo;
    @Mock PasswordEncoder encoder;
    @Mock JwtUtil jwtUtil;
    @Mock VerifyCodeService verifyCode;
    @Mock RefreshTokenStore refreshTokenStore;
    @Mock LoginAttemptService loginAttempts;

    @InjectMocks AuthService service;

    @Test
    void login_blocks_with_423_when_already_locked() {
        given(loginAttempts.isLocked("a@b.com")).willReturn(true);
        assertThatThrownBy(() -> service.loginByEmail("a@b.com", "anything"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("锁定");
        verify(userRepo, never()).findByEmail(any());
    }

    @Test
    void login_records_failure_on_bad_password() {
        given(loginAttempts.isLocked("a@b.com")).willReturn(false);
        User u = User.builder().id(1L).email("a@b.com").status("ACTIVE").passwordHash("hash").build();
        given(userRepo.findByEmail("a@b.com")).willReturn(Optional.of(u));
        given(encoder.matches("wrong", "hash")).willReturn(false);

        assertThatThrownBy(() -> service.loginByEmail("a@b.com", "wrong"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号或密码错误");

        verify(loginAttempts).recordFailure("a@b.com");
        verify(loginAttempts, never()).clear(any());
    }

    @Test
    void login_records_failure_for_unknown_email_to_avoid_enumeration_signal() {
        given(loginAttempts.isLocked("ghost@x.com")).willReturn(false);
        given(userRepo.findByEmail("ghost@x.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.loginByEmail("ghost@x.com", "any"))
                .isInstanceOf(BusinessException.class);
        verify(loginAttempts).recordFailure("ghost@x.com");
    }

    @Test
    void login_clears_failures_on_success() {
        given(loginAttempts.isLocked("a@b.com")).willReturn(false);
        User u = User.builder().id(1L).email("a@b.com").status("ACTIVE").passwordHash("hash")
                .nickname("a").role("USER").build();
        given(userRepo.findByEmail("a@b.com")).willReturn(Optional.of(u));
        given(encoder.matches("right", "hash")).willReturn(true);
        given(jwtUtil.generateAccessToken(any(), any(), any())).willReturn("at");
        given(jwtUtil.generateRefreshToken(any())).willReturn("rt");

        service.loginByEmail("a@b.com", "right");

        verify(loginAttempts).clear("a@b.com");
    }
}
