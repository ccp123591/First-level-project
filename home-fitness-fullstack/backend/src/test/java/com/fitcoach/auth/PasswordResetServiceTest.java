package com.fitcoach.auth;

import com.fitcoach.exception.BusinessException;
import com.fitcoach.infra.notify.MailSender;
import com.fitcoach.user.User;
import com.fitcoach.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordResetServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> ops;
    @Mock UserRepository userRepo;
    @Mock PasswordEncoder encoder;
    @Mock MailSender mail;

    @InjectMocks PasswordResetService service;

    @BeforeEach
    void setup() {
        given(redis.opsForValue()).willReturn(ops);
        ReflectionTestUtils.setField(service, "frontBase", "http://localhost:5173");
    }

    @Test
    void forgot_known_user_stores_token_and_sends_link() {
        User u = User.builder().id(7L).email("a@b.com").status("ACTIVE").build();
        given(userRepo.findByEmail("a@b.com")).willReturn(Optional.of(u));

        String raw = service.forgot("a@b.com");

        assertThat(raw).isNotBlank().hasSize(32);
        verify(ops).set(eq("pwreset:" + PasswordResetService.sha256(raw)), eq("7"), eq(Duration.ofMinutes(30)));
        verify(mail).send(eq("a@b.com"), contains("密码重置"), contains("/reset?token=" + raw));
    }

    @Test
    void forgot_unknown_email_silently_succeeds_no_mail_sent() {
        given(userRepo.findByEmail("nobody@x.com")).willReturn(Optional.empty());

        String raw = service.forgot("nobody@x.com");

        assertThat(raw).isNull();
        verifyNoInteractions(mail);
    }

    @Test
    void reset_with_valid_token_updates_password_and_deletes_token() {
        String raw = "deadbeef";
        String key = "pwreset:" + PasswordResetService.sha256(raw);
        given(ops.get(key)).willReturn("7");
        User u = User.builder().id(7L).email("a@b.com").passwordHash("old").build();
        given(userRepo.findById(7L)).willReturn(Optional.of(u));
        given(encoder.encode("NewPass123")).willReturn("encoded");

        service.reset(raw, "NewPass123");

        assertThat(u.getPasswordHash()).isEqualTo("encoded");
        verify(userRepo).save(u);
        verify(redis).delete(key);
    }

    @Test
    void reset_with_expired_or_unknown_token_throws_400() {
        given(ops.get(any())).willReturn(null);
        assertThatThrownBy(() -> service.reset("bogus", "GoodPass1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已过期");
    }

    @Test
    void reset_rejects_weak_password_before_lookup() {
        assertThatThrownBy(() -> service.reset("any", "short"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码至少 8 位");
        verifyNoInteractions(userRepo);
    }

    @Test
    void reset_rejects_password_without_letter_and_digit() {
        assertThatThrownBy(() -> service.reset("any", "12345678"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("字母与数字");
    }
}
