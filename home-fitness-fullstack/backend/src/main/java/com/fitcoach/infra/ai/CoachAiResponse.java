package com.fitcoach.infra.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 教练统一返回结构 — 对应 strict-JSON 输出的 4 个核心字段 + 元数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachAiResponse {
    /** 点评（针对完成情况、节奏、姿态）。 */
    private String review;
    /** 改进建议（具体动作要点）。 */
    private String suggestion;
    /** 鼓励语（短，一句话）。 */
    private String encouragement;
    /** 下次目标（数字 + 动作）。 */
    private String nextGoal;

    /** 实现名：mock / mimo。 */
    private String provider;
    /** 该次调用消耗 tokens（mock 返回 0）。 */
    private Integer tokensUsed;
}
