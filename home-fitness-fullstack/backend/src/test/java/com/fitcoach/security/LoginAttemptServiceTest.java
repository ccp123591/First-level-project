package com.fitcoach.security;

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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginAttemptServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> ops;

    @InjectMocks LoginAttemptService service;

    @BeforeEach
    void setup() {
        given(redis.opsForValue()).willReturn(ops);
        ReflectionTestUtils.setField(service, "maxAttempts", 5);
        ReflectionTestUtils.setField(service, "lockWindowMinutes", 15);
    }

    @Test
    void recordFailure_increments_and_sets_ttl() {
        given(ops.increment("login:fail:x@y.com")).willReturn(3L);
        service.recordFailure("x@y.com");
        verify(ops).increment("login:fail:x@y.com");
        verify(redis).expire(eq("login:fail:x@y.com"), eq(Duration.ofMinutes(15)));
    }

    @Test
    void isLocked_true_when_count_at_or_above_threshold() {
        given(ops.get("login:fail:x@y.com")).willReturn("5");
        assertThat(service.isLocked("x@y.com")).isTrue();

        given(ops.get("login:fail:x@y.com")).willReturn("6");
        assertThat(service.isLocked("x@y.com")).isTrue();
    }

    @Test
    void isLocked_false_when_below_threshold_or_missing() {
        given(ops.get("login:fail:x@y.com")).willReturn("4");
        assertThat(service.isLocked("x@y.com")).isFalse();

        given(ops.get("login:fail:other")).willReturn(null);
        assertThat(service.isLocked("other")).isFalse();
    }

    @Test
    void redis_failure_fails_open_returning_false() {
        given(ops.get("login:fail:x@y.com")).willThrow(new RuntimeException("down"));
        assertThat(service.isLocked("x@y.com")).isFalse();
    }

    @Test
    void clear_deletes_key() {
        service.clear("x@y.com");
        verify(redis).delete("login:fail:x@y.com");
    }
}
