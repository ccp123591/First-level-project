package com.fitcoach.auth;

import com.fitcoach.exception.BusinessException;
import com.fitcoach.infra.notify.MailSender;
import com.fitcoach.infra.notify.SmsSender;
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

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VerifyCodeServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> ops;
    @Mock SmsSender sms;
    @Mock MailSender mail;

    @InjectMocks VerifyCodeService service;

    @BeforeEach
    void wire() {
        given(redis.opsForValue()).willReturn(ops);
    }

    @Test
    void generate_sms_stores_hash_sets_cooldown_and_sends() {
        // 无冷却
        given(redis.hasKey("verify:sent:sms:13900000000")).willReturn(false);

        String raw = service.generate("sms", "13900000000", "login");

        assertThat(raw).hasSize(6);
        assertThat(raw).matches("\\d{6}");
        // code key 写入
        verify(ops).set(eq("verify:sms:13900000000"), anyString(), eq(Duration.ofMinutes(5)));
        // cooldown 写入
        verify(ops).set(eq("verify:sent:sms:13900000000"), anyString(), eq(Duration.ofSeconds(60)));
        // 短信发出
        verify(sms).send("13900000000", raw, "login");
        verifyNoInteractions(mail);
    }

    @Test
    void generate_email_uses_mail_sender() {
        given(redis.hasKey("verify:sent:email:a@b.com")).willReturn(false);

        String raw = service.generate("email", "a@b.com", "reset");

        verify(mail).sendCode("a@b.com", raw, "reset");
        verifyNoInteractions(sms);
    }

    @Test
    void generate_during_cooldown_throws_429() {
        given(redis.hasKey("verify:sent:sms:13900000000")).willReturn(true);

        assertThatThrownBy(() -> service.generate("sms", "13900000000", "login"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("发送过于频繁");
        verifyNoInteractions(sms);
    }

    @Test
    void verify_with_correct_code_consumes_and_returns_true() {
        String raw = "123456";
        String hashed = VerifyCodeService.hash(raw, "sms", "13900000000", "login");
        given(ops.get("verify:sms:13900000000")).willReturn(hashed);

        boolean ok = service.verify("sms", "13900000000", "login", raw);

        assertThat(ok).isTrue();
        verify(redis).delete("verify:sms:13900000000");
    }

    @Test
    void verify_with_wrong_code_returns_false() {
        given(ops.get("verify:sms:13900000000")).willReturn("some-other-hash");
        boolean ok = service.verify("sms", "13900000000", "login", "000000");
        assertThat(ok).isFalse();
    }

    @Test
    void verify_with_no_stored_code_returns_false() {
        given(ops.get("verify:sms:13900000000")).willReturn(null);
        boolean ok = service.verify("sms", "13900000000", "login", "123456");
        assertThat(ok).isFalse();
    }

    @Test
    void generate_rejects_unknown_channel() {
        assertThatThrownBy(() -> service.generate("postal", "x", "login"))
                .isInstanceOf(BusinessException.class);
    }
}
