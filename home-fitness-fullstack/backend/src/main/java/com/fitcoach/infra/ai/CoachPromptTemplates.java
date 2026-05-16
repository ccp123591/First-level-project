package com.fitcoach.infra.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Prompt 模板 — 系统 prompt 强约束 strict-JSON 输出，user prompt 是上下文 JSON dump。
 * 模板对所有 provider 共用（mock 不调，但保留以便日志/调试一致）。
 */
public final class CoachPromptTemplates {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CoachPromptTemplates() {}

    public static final String SYSTEM_FEEDBACK = """
            你是一名专业居家健身教练。请基于用户给出的本次训练数据，给出中文反馈。
            必须严格输出 JSON 对象，不要任何额外文本，结构如下：
            {"review":"...","suggestion":"...","encouragement":"...","nextGoal":"..."}
            约束：
            - review ≤ 80 字，针对本次完成情况 / 节奏 / 姿态分维度。
            - suggestion ≤ 80 字，给出可执行改进点。
            - encouragement ≤ 30 字，正向激励。
            - nextGoal ≤ 30 字，形如 "下次目标：12 次 深蹲"。
            """;

    public static final String SYSTEM_SUGGESTION = """
            你是一名专业居家健身教练。请基于用户近 7 次训练的聚合数据，给出综合训练建议。
            必须严格输出 JSON 对象，不要任何额外文本，结构如下：
            {"review":"...","suggestion":"...","encouragement":"...","nextGoal":"..."}
            约束：
            - review 总结整体训练趋势。
            - suggestion 给出 3 条可执行建议（用顿号分隔）。
            - encouragement 一句正向激励。
            - nextGoal 一句本周目标。
            """;

    public static final String SYSTEM_WEEKLY_PLAN = """
            你是一名专业居家健身教练。请基于用户近 7 次训练数据，给出未来一周训练计划。
            必须严格输出 JSON 对象，不要任何额外文本，结构如下：
            {"review":"...","suggestion":"...","encouragement":"...","nextGoal":"..."}
            约束：
            - review 总结本周建议训练频次（如：每周 3-4 次）。
            - suggestion 列出本周训练的动作 + 次数（顿号分隔，如：周一 深蹲 15、周三 俯卧撑 12）。
            - encouragement 一句正向激励。
            - nextGoal 一句本周训练目标。
            """;

    public static String userPrompt(CoachContext ctx) {
        try {
            return MAPPER.writeValueAsString(ctx);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
