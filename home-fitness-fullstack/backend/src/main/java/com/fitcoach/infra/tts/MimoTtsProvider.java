package com.fitcoach.infra.tts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fitcoach.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MiMo TTS — 走小米 MiMo 的 /v1/chat/completions（model=mimo-v2-tts），
 * 不是 OpenAI 标准的 /v1/audio/speech。
 *
 * Body 形态：
 *   {
 *     "model": "mimo-v2-tts",
 *     "messages": [
 *       {"role": "user", "content": "<引导短语>"},
 *       {"role": "assistant", "content": "<要念的文本>"}
 *     ],
 *     "audio": {"format": "wav", "voice": "default_zh"}
 *   }
 *
 * 响应：choices[0].message.audio.data 是 base64 编码的音频。
 *
 * 配置：
 *   ai.tts.mimo.api-key
 *   ai.tts.mimo.base-url    （默认 https://api.xiaomimimo.com/v1）
 *   ai.tts.mimo.model       （默认 mimo-v2-tts）
 *   ai.tts.mimo.voice       （默认 default_zh）
 *   ai.tts.mimo.format      （默认 wav；可选 mp3）
 *
 * api-key 空时 isAvailable() 返回 false → 由 TtsService 自动 fallback。
 */
@Slf4j
@Component
public class MimoTtsProvider implements TtsProvider {

    private final String apiKey;
    private final String baseUrl;
    private final String defaultModel;
    private final String defaultVoice;
    private final String defaultFormat;
    private final RestClient restClient;

    public MimoTtsProvider(
            @Value("${ai.tts.mimo.api-key:}") String apiKey,
            @Value("${ai.tts.mimo.base-url:https://api.xiaomimimo.com/v1}") String baseUrl,
            @Value("${ai.tts.mimo.model:mimo-v2-tts}") String model,
            @Value("${ai.tts.mimo.voice:default_zh}") String voice,
            @Value("${ai.tts.mimo.format:wav}") String format,
            RestTemplateBuilder builder) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.defaultModel = model;
        this.defaultVoice = voice;
        this.defaultFormat = format;
        this.restClient = RestClient.builder(builder.build()).build();
    }

    @PostConstruct
    void init() {
        log.info("[tts:mimo] init: baseUrl={} model={} voice={} format={} api-key={}",
                baseUrl, defaultModel, defaultVoice, defaultFormat,
                apiKey.isBlank() ? "<empty, will fallback>" : "<set>");
    }

    @Override
    public String name() { return "mimo"; }

    @Override
    public boolean isAvailable() {
        return !apiKey.isBlank();
    }

    @Override
    public TtsResult speak(String text, String voice) {
        if (!isAvailable()) {
            throw new BusinessException(503, "MiMo TTS unavailable: api-key 未配置");
        }
        if (text == null || text.isBlank()) {
            throw new BusinessException(400, "TTS text 不能为空");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", defaultModel);
        body.put("messages", List.of(
                Map.of("role", "user", "content", "请把下面这句话用自然的语气念出来。"),
                Map.of("role", "assistant", "content", text)
        ));
        body.put("audio", Map.of(
                "format", defaultFormat,
                "voice", voice == null || voice.isBlank() ? defaultVoice : voice
        ));

        try {
            JsonNode root = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .headers(h -> h.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null) {
                throw new BusinessException(503, "MiMo TTS 返回空响应");
            }
            String base64 = root.path("choices").path(0).path("message").path("audio").path("data").asText("");
            if (base64.isBlank()) {
                log.warn("[tts:mimo] 响应缺少 audio.data: {}", root);
                throw new BusinessException(503, "MiMo TTS 返回无音频");
            }

            // 估算时长：base64 解码后字节数 / (采样率 * 2 字节)，default WAV 是 24kHz 16-bit mono → 48000 bytes/sec
            long bytes = base64.length() * 3L / 4L;
            double estDur = "wav".equalsIgnoreCase(defaultFormat)
                    ? Math.round(bytes / 48000.0 * 10) / 10.0
                    : Math.round(bytes / 16000.0 * 10) / 10.0;

            return TtsResult.builder()
                    .audioBase64(base64)
                    .mimeType("wav".equalsIgnoreCase(defaultFormat) ? "audio/wav" : "audio/mpeg")
                    .fallbackText(text)
                    .provider(name())
                    .durationSec(estDur)
                    .build();
        } catch (RestClientException e) {
            log.warn("[tts:mimo] HTTP failure: {}", e.getMessage());
            throw new BusinessException(503, "MiMo TTS unavailable");
        }
    }
}
