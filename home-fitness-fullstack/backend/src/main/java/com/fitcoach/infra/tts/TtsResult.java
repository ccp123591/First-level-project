package com.fitcoach.infra.tts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TTS 输出统一结构。
 * 优先返回 audioBase64（前端 Audio 直接 src=data:URL 播放）；
 * 当 provider 是浏览器 fallback 时，audioBase64 为 null，前端用 speechSynthesis 朗读 fallbackText。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsResult {
    /** base64 编码的音频字节；null 表示走前端浏览器 fallback 朗读 fallbackText。 */
    private String audioBase64;
    /** audio MIME；audio/mpeg / audio/wav；audioBase64==null 时为 null。 */
    private String mimeType;
    /** 原始文本，前端无音频时朗读。 */
    private String fallbackText;
    /** mimo / browser-fallback / mock */
    private String provider;
    /** 估算时长（秒），可空。 */
    private Double durationSec;
}
