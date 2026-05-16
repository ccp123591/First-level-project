package com.fitcoach.emotion;

import com.fitcoach.common.ApiResult;
import com.fitcoach.common.PageResult;
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

/**
 * 情感计算 REST 入口。
 */
@Tag(name = "06. 情感计算", description = "用户文本情感分析、历史与汇总")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/emotion")
@RequiredArgsConstructor
public class EmotionController {

    private final EmotionService emotionService;

    @Operation(summary = "分析一段文本并持久化结果")
    @PostMapping("/analyze")
    public ApiResult<EmotionResponse> analyze(@Valid @RequestBody AnalyzeRequest req) {
        Long userId = SecurityUtil.currentUserId();
        return ApiResult.ok(emotionService.analyze(userId,
                req.getSource(), req.getRefId(), req.getText()));
    }

    @Operation(summary = "历史记录（分页）")
    @GetMapping("/history")
    public ApiResult<PageResult<EmotionResponse>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtil.currentUserId();
        return ApiResult.ok(emotionService.history(userId, page, size));
    }

    @Operation(summary = "近 N 天情感汇总")
    @GetMapping("/summary")
    public ApiResult<EmotionSummary> summary(@RequestParam(defaultValue = "7") int days) {
        Long userId = SecurityUtil.currentUserId();
        return ApiResult.ok(emotionService.summary(userId, days));
    }

    @Data
    public static class AnalyzeRequest {
        @NotBlank
        @Size(max = 1000)
        private String text;
        /** note / post / chat / session / generic */
        private String source;
        private Long refId;
    }
}
