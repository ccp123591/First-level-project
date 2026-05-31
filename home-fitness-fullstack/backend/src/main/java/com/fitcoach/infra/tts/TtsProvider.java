package com.fitcoach.infra.tts;

/**
 * 文本转语音抽象。两路实现：
 *   MimoTtsProvider           — 调小米 MiMo TTS 云端 API，返回 mp3 base64
 *   BrowserFallbackTtsProvider — 不调外服务，前端用 Web Speech API 朗读
 * 由 ai.tts.provider 配置选择。
 */
public interface TtsProvider {
    String name();
    TtsResult speak(String text, String voice);
    /** 服务是否可用（用于 mimo 自动 fallback 判断）。 */
    boolean isAvailable();
}
