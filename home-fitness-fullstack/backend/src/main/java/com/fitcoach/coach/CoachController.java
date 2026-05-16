package com.fitcoach.coach;

import com.fitcoach.common.ApiResult;
import com.fitcoach.common.PageResult;
import com.fitcoach.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI 教练 Controller — 全部委托给 CoachService。
 */
@Tag(name = "04. AI 教练", description = "AI 驱动的训练点评、建议与计划")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/coach")
@RequiredArgsConstructor
public class CoachController {

    private final CoachService coachService;

    @Operation(summary = "生成训练后反馈")
    @PostMapping("/feedback")
    public ApiResult<FeedbackResponse> feedback(@Valid @RequestBody FeedbackRequest req) {
        Long userId = SecurityUtil.currentUserId();
        return ApiResult.ok(coachService.feedback(userId, req.getSessionId()));
    }

    @Operation(summary = "获取综合训练建议（基于近 7 次）")
    @GetMapping("/suggestion")
    public ApiResult<FeedbackResponse> suggestion() {
        Long userId = SecurityUtil.currentUserId();
        return ApiResult.ok(coachService.suggestion(userId));
    }

    @Operation(summary = "生成本周训练计划")
    @GetMapping("/weekly-plan")
    public ApiResult<FeedbackResponse> weeklyPlan() {
        Long userId = SecurityUtil.currentUserId();
        return ApiResult.ok(coachService.weeklyPlan(userId));
    }

    @Operation(summary = "历史反馈列表（分页）")
    @GetMapping("/history")
    public ApiResult<PageResult<FeedbackResponse>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtil.currentUserId();
        return ApiResult.ok(coachService.history(userId, page, size));
    }

    @Data
    public static class FeedbackRequest {
        @NotNull
        private Long sessionId;
    }
}
