package com.fitcoach.tts;

import com.fitcoach.common.ApiResult;
import com.fitcoach.infra.tts.TtsResult;
import com.fitcoach.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "11. 语音 TTS", description = "文本转语音 - mimo / browser 双方案")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
public class TtsController {

    private final TtsService service;

    @Operation(summary = "通用 TTS：把任意短文本合成为语音")
    @PostMapping("/speak")
    public ApiResult<TtsResult> speak(@Valid @RequestBody SpeakRequest req) {
        return ApiResult.ok(service.speak(req.getText(), req.getVoice()));
    }

    @Operation(summary = "把指定教练反馈整段播报")
    @PostMapping("/coach/feedback/{id}")
    public ApiResult<TtsResult> speakFeedback(@PathVariable Long id) {
        return ApiResult.ok(service.speakFeedback(SecurityUtil.currentUserId(), id));
    }

    @Data
    public static class SpeakRequest {
        @NotBlank
        @Size(max = 500)
        private String text;
        private String voice;
    }
}
