package com.fitcoach.infra.tts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * 浏览器 fallback：服务端不生成音频，把文本作为 fallbackText 返回。
 * 前端 useSpeechSynthesis（Web Speech API）朗读。
 * 默认 @Order 兜底，永远可用 — Mimo 不可用时自动用它。
 */
@Slf4j
@Component
public class BrowserFallbackTtsProvider implements TtsProvider {

    @Override
    public String name() {
        return "browser-fallback";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public TtsResult speak(String text, String voice) {
        String safe = text == null ? "" : text.trim();
        return TtsResult.builder()
                .audioBase64(null)
                .mimeType(null)
                .fallbackText(safe)
                .provider(name())
                .durationSec(estimateDuration(safe))
                .build();
    }

    /** 中文按 5 字/秒估算 — 给前端做 UI 时长预估。 */
    private static double estimateDuration(String text) {
        if (text == null || text.isEmpty()) return 0.0;
        return Math.round(text.length() / 5.0 * 10) / 10.0;
    }

    /** 单测用 — 给定 bytes 直接 base64。 */
    static String b64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
