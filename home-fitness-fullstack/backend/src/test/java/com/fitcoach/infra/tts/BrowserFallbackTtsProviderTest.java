package com.fitcoach.infra.tts;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BrowserFallbackTtsProviderTest {

    private final BrowserFallbackTtsProvider provider = new BrowserFallbackTtsProvider();

    @Test
    void always_available() {
        assertThat(provider.isAvailable()).isTrue();
        assertThat(provider.name()).isEqualTo("browser-fallback");
    }

    @Test
    void speak_returns_text_only_no_audio() {
        TtsResult r = provider.speak("你好世界", null);
        assertThat(r.getAudioBase64()).isNull();
        assertThat(r.getMimeType()).isNull();
        assertThat(r.getFallbackText()).isEqualTo("你好世界");
        assertThat(r.getProvider()).isEqualTo("browser-fallback");
        assertThat(r.getDurationSec()).isPositive();
    }

    @Test
    void speak_handles_null_text_safely() {
        TtsResult r = provider.speak(null, null);
        assertThat(r.getFallbackText()).isEmpty();
        assertThat(r.getDurationSec()).isZero();
    }
}
