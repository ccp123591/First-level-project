package com.fitcoach.infra.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 拼装给 AI 的上下文。null 字段允许 — 由 prompt 模板按存在性裁剪。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachContext {

    private Long userId;
    private String nickname;

    // —— 本次 session（feedback 用） ——
    private String action;
    private String actionLabel;
    private Integer reps;
    private Integer targetReps;
    private Integer duration;
    private Integer score;
    private Integer rhythmScore;
    private Integer stabilityScore;
    private Integer depthScore;
    private Integer symmetryScore;
    private Integer completionScore;

    // —— 近期聚合（suggestion / weeklyPlan 用） ——
    private Integer recentAvgScore;
    private Long recentTotalReps;
    private List<RecentSession> recentSessions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentSession {
        private String date;
        private String action;
        private Integer reps;
        private Integer score;
    }
}
