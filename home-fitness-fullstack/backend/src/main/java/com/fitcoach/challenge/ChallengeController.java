package com.fitcoach.challenge;

import com.fitcoach.common.ApiResult;
import com.fitcoach.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "08. 挑战赛", description = "活跃挑战列表 / 报名 / 排行榜")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService service;

    @Operation(summary = "活跃挑战列表")
    @GetMapping
    public ApiResult<List<ChallengeResponse>> list() {
        return ApiResult.ok(service.listActive(SecurityUtil.currentUserIdOrNull()));
    }

    @Operation(summary = "挑战详情")
    @GetMapping("/{id}")
    public ApiResult<ChallengeResponse> detail(@PathVariable Long id) {
        return ApiResult.ok(service.detail(id, SecurityUtil.currentUserIdOrNull()));
    }

    @Operation(summary = "报名挑战（幂等）")
    @PostMapping("/{id}/join")
    public ApiResult<ChallengeResponse> join(@PathVariable Long id) {
        return ApiResult.ok(service.join(SecurityUtil.currentUserId(), id));
    }

    @Operation(summary = "挑战排行榜")
    @GetMapping("/{id}/rank")
    public ApiResult<List<ChallengeRankRow>> rank(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "20") int limit) {
        return ApiResult.ok(service.rank(id, limit));
    }
}
