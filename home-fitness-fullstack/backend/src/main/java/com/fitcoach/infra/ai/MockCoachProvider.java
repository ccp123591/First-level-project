package com.fitcoach.infra.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 默认 AI 教练实现 — 离线 / 无 API key 时启用，给出确定性的中文反馈。
 * matchIfMissing=true → 未配置 ai.coach.provider 时也启用。
 */
@Component
@ConditionalOnProperty(name = "ai.coach.provider", havingValue = "mock", matchIfMissing = true)
public class MockCoachProvider implements AiCoachProvider {

    private static final Pattern P_LOW      = Pattern.compile("(累|烦|不开心|难过|疲惫|压力|焦虑|想哭|心情不好|emo|沮丧|郁闷|崩溃|无力)");
    private static final Pattern P_HAPPY    = Pattern.compile("(开心|快乐|高兴|爽|超棒|太棒|完成|搞定|赞|nice)");
    private static final Pattern P_TRAIN    = Pattern.compile("(练|训练|动作|深蹲|俯卧|平板|计划|目标|增肌|减脂|减肥|强度)");
    private static final Pattern P_MEMORY   = Pattern.compile("(记得|记不记得|上次|之前|提过|说过|那天|那次)");
    private static final Pattern P_GREETING = Pattern.compile("(你好|嗨|hello|hi|在吗|在不在|早|晚安|午安)");

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

    @Override
    public ChatAiResponse chat(CoachContext ctx, String userMessage, List<ChatTurn> history) {
        String msg = userMessage == null ? "" : userMessage.trim();
        String lower = msg.toLowerCase();
        boolean hasMemory = ctx.getRelevantHistory() != null && !ctx.getRelevantHistory().isBlank();
        String nick = ctx.getNickname();
        String nickComma = (nick != null && !nick.isBlank()) ? "，" + nick : "";

        StringBuilder sb = new StringBuilder();
        if (P_LOW.matcher(lower).find()) {
            sb.append("听你这么说，心里跟着也沉了一下");
            sb.append(nickComma).append("。");
            if (hasMemory) {
                sb.append("我还记得你之前也有过类似的时候，那时候稍微动一动反而好多了。");
            }
            sb.append("不用强求今天非练不可，先去喝口水深呼吸三下，行吗？");
        } else if (P_HAPPY.matcher(lower).find()) {
            sb.append("你这状态我隔着屏幕都能感受到！");
            sb.append("要不趁这股劲儿，加 3 个 ").append(labelOf(ctx)).append(" 再收？");
        } else if (P_MEMORY.matcher(lower).find()) {
            if (hasMemory) {
                sb.append("当然记得，你之前提过的这些我都收着：\n");
                sb.append(ctx.getRelevantHistory());
                sb.append("\n现在再回头看，感觉怎么样？");
            } else {
                sb.append("嗯…这件事我好像没具体收到过，要不你跟我说说？以后我帮你记着。");
            }
        } else if (P_TRAIN.matcher(lower).find()) {
            sb.append("好呀，结合你最近的状态");
            Integer avg = ctx.getRecentAvgScore();
            if (avg != null && avg > 0) sb.append("（平均分 ").append(avg).append("）");
            sb.append("，下次比上回多做 2-3 个，节奏稳为先。");
            if (hasMemory) sb.append(" 之前你也是这样上来的，相信自己。");
        } else if (P_GREETING.matcher(lower).find()) {
            sb.append("嗨").append(nickComma).append("，我在的。");
            sb.append("今天想随便聊聊，还是练几个动作放松一下？");
        } else {
            sb.append("嗯，听到你了").append(nickComma).append("。");
            if (hasMemory) {
                sb.append("说到这个，我想起来你之前提过 — ");
                String[] lines = ctx.getRelevantHistory().split("\n");
                if (lines.length > 0) sb.append(lines[0].replace("- ", "").trim()).append("。");
            }
            sb.append("你接着说，我听着。");
        }

        return ChatAiResponse.builder()
                .reply(sb.toString())
                .provider(name())
                .tokensUsed(0)
                .build();
    }
}
