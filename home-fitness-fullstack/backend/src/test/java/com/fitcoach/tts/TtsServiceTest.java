package com.fitcoach.tts;

import com.fitcoach.coach.CoachFeedback;
import com.fitcoach.coach.CoachFeedbackRepository;
import com.fitcoach.exception.BusinessException;
import com.fitcoach.infra.tts.BrowserFallbackTtsProvider;
import com.fitcoach.infra.tts.MimoTtsProvider;
import com.fitcoach.infra.tts.TtsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TtsServiceTest {

    @Mock MimoTtsProvider mimo;
    @Mock BrowserFallbackTtsProvider browser;
    @Mock CoachFeedbackRepository fbRepo;

    @InjectMocks TtsService service;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "configured", "mimo");
        given(browser.speak(any(), any())).willReturn(
                TtsResult.builder().fallbackText("x").provider("browser-fallback").build());
    }

    @Test
    void uses_mimo_when_configured_and_available() {
        given(mimo.isAvailable()).willReturn(true);
        given(mimo.speak("hi", null)).willReturn(
                TtsResult.builder().audioBase64("AAA").mimeType("audio/mpeg").provider("mimo").build());

        TtsResult r = service.speak("hi", null);

        assertThat(r.getProvider()).isEqualTo("mimo");
        verify(browser, never()).speak(any(), any());
    }

    @Test
    void fallback_when_mimo_unavailable() {
        given(mimo.isAvailable()).willReturn(false);
        TtsResult r = service.speak("hi", null);
        assertThat(r.getProvider()).isEqualTo("browser-fallback");
        verify(mimo, never()).speak(any(), any());
    }

    @Test
    void fallback_when_mimo_throws() {
        given(mimo.isAvailable()).willReturn(true);
        given(mimo.speak("hi", null)).willThrow(new BusinessException(503, "down"));
        TtsResult r = service.speak("hi", null);
        assertThat(r.getProvider()).isEqualTo("browser-fallback");
    }

    @Test
    void uses_browser_directly_when_configured() {
        ReflectionTestUtils.setField(service, "configured", "browser");
        TtsResult r = service.speak("hi", null);
        assertThat(r.getProvider()).isEqualTo("browser-fallback");
        verify(mimo, never()).speak(any(), any());
    }

    @Test
    void rejects_blank_text() {
        assertThatThrownBy(() -> service.speak("  ", null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void speak_feedback_403_when_other_user() {
        CoachFeedback fb = new CoachFeedback();
        fb.setId(1L); fb.setUserId(999L); fb.setReview("r");
        given(fbRepo.findById(1L)).willReturn(Optional.of(fb));
        assertThatThrownBy(() -> service.speakFeedback(7L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void speak_feedback_concatenates_all_fields() {
        CoachFeedback fb = new CoachFeedback();
        fb.setId(1L); fb.setUserId(7L);
        fb.setReview("R"); fb.setSuggestion("S"); fb.setEncouragement("E"); fb.setNextGoal("G");
        given(fbRepo.findById(1L)).willReturn(Optional.of(fb));
        ReflectionTestUtils.setField(service, "configured", "browser");
        given(browser.speak(any(), any())).willAnswer(inv ->
                TtsResult.builder().fallbackText(inv.getArgument(0)).provider("browser-fallback").build());

        TtsResult r = service.speakFeedback(7L, 1L);
        assertThat(r.getFallbackText()).contains("R", "S", "E", "G");
    }
}
