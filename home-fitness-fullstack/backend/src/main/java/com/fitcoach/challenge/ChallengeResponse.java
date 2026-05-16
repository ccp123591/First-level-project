package com.fitcoach.challenge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 挑战赛对外结构（含当前用户进度，未登录时进度字段为 null）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeResponse {
    private Long id;
    private String title;
    private String description;
    private String action;
    private Integer targetReps;
    private String startDate;
    private String endDate;
    private String status;
    private String cover;
    private LocalDateTime createdAt;

    // —— 当前用户视图 ——
    private Boolean joined;
    private Integer myProgress;
    private Boolean myCompleted;

    // —— 全局统计 ——
    private Long participantCount;
}
