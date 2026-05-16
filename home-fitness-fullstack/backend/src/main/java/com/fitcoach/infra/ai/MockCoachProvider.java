package com.fitcoach.infra.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 默认 AI 教练实现 — 离线 / 无 API key 时启用，给出确定性的中文反馈。
 * matchIfMissing=true → 未配置 ai.coach.provider 时也启用。
 */
@Component
@ConditionalOnProperty(name = "ai.coach.provider", havingValue = "mock", matchIfMissing = true)
public class MockCoachProvider implements AiCoachProvider {

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public CoachAiResponse feedback(CoachContext ctx) {
        String label = labelOf(ctx);
        Integer reps = ctx.getReps() == null ? 0 : ctx.getReps();
        Integer score = ctx.getScore() == null ? 0 : ctx.getScore();

        String review;
        if (score >= 90) {
            review = String.format("本次 %s %d 次，得分 %d，动作非常标准，节奏稳定。", label, reps, score);
        } else if (score >= 75) {
            review = String.format("本次 %s %d 次，得分 %d，整体不错，节奏与深度可再提升。", label, reps, score);
        } else if (score >= 60) {
            review = String.format("本次 %s %d 次，得分 %d，动作合格，建议关注稳定性与发力均衡。", label, reps, score);
        } else {
            review = String.format("本次 %s %d 次，得分 %d，动作偏粗糙，建议放慢节奏、关注关键角度。", label, reps, score);
        }

        String suggestion;
        if (score >= 80) {
            suggestion = String.format("可适当加大幅度或提高频率，下次目标 %d 次。", reps + 3);
        } else {
            suggestion = "放慢节奏、控制底位 1 秒，注意左右发力均衡。";
        }

        String encouragement = score >= 75 ? "保持节奏，明天再战！" : "稳扎稳打，进步看得见！";
        String nextGoal = String.format("下次目标：%d 次 %s", Math.max(reps + 2, 8), label);

        return CoachAiResponse.builder()
                .review(review)
                .suggestion(suggestion)
                .encouragement(encouragement)
                .nextGoal(nextGoal)
                .provider(name())
                .tokensUsed(0)
                .build();
    }

    @Override
    public CoachAiResponse suggestion(CoachContext ctx) {
        Integer avg = ctx.getRecentAvgScore() == null ? 0 : ctx.getRecentAvgScore();
        long total = ctx.getRecentTotalReps() == null ? 0L : ctx.getRecentTotalReps();
        String review = String.format("近期平均得分 %d，累计完成 %d 次，整体训练状态稳定。", avg, total);
        String suggestion = "周训 3-4 次、加入有氧 10 分钟、保持核心训练频率。";
        String encouragement = "持续就有回报，继续保持！";
        String nextGoal = "本周目标：3 次有效训练 + 200 次累计动作";
        return CoachAiResponse.builder()
                .review(review).suggestion(suggestion)
                .encouragement(encouragement).nextGoal(nextGoal)
                .provider(name()).tokensUsed(0).build();
    }

    @Override
    public CoachAiResponse weeklyPlan(CoachContext ctx) {
        String review = "建议本周训练 3-4 次，覆盖下肢 / 上肢 / 核心三大块。";
        String suggestion = "周一 深蹲 15、周三 俯卧撑 12、周五 平板支撑 60s、周日 前屈伸展 20。";
        String encouragement = "规律训练才是王道，加油！";
        String nextGoal = "本周完成 4 次训练并积累 ≥ 200 reps。";
        return CoachAiResponse.builder()
                .review(review).suggestion(suggestion)
                .encouragement(encouragement).nextGoal(nextGoal)
                .provider(name()).tokensUsed(0).build();
    }

    private static String labelOf(CoachContext ctx) {
        if (ctx.getActionLabel() != null && !ctx.getActionLabel().isBlank()) return ctx.getActionLabel();
        if (ctx.getAction() != null) {
            return switch (ctx.getAction()) {
                case "squat" -> "深蹲";
                case "pushup" -> "俯卧撑";
                case "plank" -> "平板支撑";
                case "stretch" -> "前屈伸展";
                default -> ctx.getAction();
            };
        }
        return "训练";
    }
}
