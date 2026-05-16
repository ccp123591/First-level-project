package com.fitcoach.infra.ai;

/**
 * AI 教练抽象 — feedback / suggestion / weeklyPlan 对应业务三类调用，
 * 由 ai.coach.provider 配置选择具体实现（mock / mimo）。
 */
public interface AiCoachProvider {

    /** 实现名（mock / mimo）。 */
    String name();

    /** 单次训练反馈：基于本次 session 的明细打分。 */
    CoachAiResponse feedback(CoachContext ctx);

    /** 综合训练建议：基于近 7 次聚合（recentAvgScore / recentSessions）。 */
    CoachAiResponse suggestion(CoachContext ctx);

    /** 本周训练计划建议：与 suggestion 类似，但更偏向多日规划。 */
    CoachAiResponse weeklyPlan(CoachContext ctx);
}
