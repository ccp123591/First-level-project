package com.fitcoach.leaderboard;

import com.fitcoach.common.ApiResult;
import com.fitcoach.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "07. 排行榜", description = "周榜 / 月榜 / 好友榜（60s Redis 缓存）")
@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @Operation(summary = "本周排行榜")
    @GetMapping("/weekly")
    public ApiResult<List<Map<String, Object>>> weekly() {
        return ApiResult.ok(leaderboardService.weekly());
    }

    @Operation(summary = "本月排行榜")
    @GetMapping("/monthly")
    public ApiResult<List<Map<String, Object>>> monthly() {
        return ApiResult.ok(leaderboardService.monthly());
    }

    @Operation(summary = "好友排行榜（本周）")
    @GetMapping("/friends")
    public ApiResult<List<Map<String, Object>>> friends() {
        return ApiResult.ok(leaderboardService.friends(SecurityUtil.currentUserIdOrNull()));
    }
}
