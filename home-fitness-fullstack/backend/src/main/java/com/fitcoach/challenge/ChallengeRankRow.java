package com.fitcoach.challenge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 挑战赛排行榜单行（按 progressReps 倒序）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeRankRow {
    private Integer rank;
    private Long userId;
    private String nickname;
    private String avatar;
    private Integer progressReps;
    private Boolean completed;
}
