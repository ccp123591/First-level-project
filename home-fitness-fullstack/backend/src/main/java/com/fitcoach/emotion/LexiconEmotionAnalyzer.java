package com.fitcoach.emotion;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 词典法情感分析 — 简单、确定性、零依赖。中文 + 英文双语词表。
 * 适合：训练笔记、社区评论等短文本。生产可叠加 LLM analyzer。
 */
@Component
@ConditionalOnProperty(name = "emotion.analyzer", havingValue = "lexicon", matchIfMissing = true)
public class LexiconEmotionAnalyzer implements EmotionAnalyzer {

    /** 积极词。命中即贡献 +1 分。 */
    private static final Set<String> POSITIVE = Set.of(
            // 中文
            "棒", "太棒了", "开心", "快乐", "满意", "成就感", "进步", "加油",
            "厉害", "完成", "突破", "提升", "状态好", "状态非常好", "舒服",
            "兴奋", "享受", "顺利", "完美", "稳定", "轻松", "好极了",
            // 英文（保持小写，分词时小写化）
            "great", "good", "awesome", "amazing", "excellent", "proud",
            "strong", "happy", "excited", "satisfied", "energized"
    );

    /** 负面词。命中即贡献 -1 分。 */
    private static final Set<String> NEGATIVE = Set.of(
            // 中文
            "累", "疲惫", "难受", "痛", "酸痛", "失败", "沮丧", "放弃",
            "受伤", "扭到", "崩溃", "坚持不下去", "受不了", "糟糕", "不行",
            "失望", "无聊", "厌倦", "焦虑", "压力大",
            // 英文
            "tired", "exhausted", "painful", "sore", "fail", "failed",
            "frustrated", "sad", "give up", "injured", "weak", "stressed"
    );

    @Override
    public String name() {
        return "lexicon";
    }

    @Override
    public EmotionResult analyze(String text) {
        if (text == null) text = "";
        String lower = text.toLowerCase();

        List<String> hits = new ArrayList<>();
        int pos = 0, neg = 0;
        for (String w : POSITIVE) {
            int c = countOccurrences(lower, w);
            if (c > 0) {
                pos += c;
                hits.add(w);
            }
        }
        for (String w : NEGATIVE) {
            int c = countOccurrences(lower, w);
            if (c > 0) {
                neg += c;
                hits.add(w);
            }
        }

        double score;
        String emotion;
        if (pos == 0 && neg == 0) {
            score = 0.0;
            emotion = "neutral";
        } else {
            score = (double) (pos - neg) / (pos + neg);
            if (score > 0.15) emotion = "positive";
            else if (score < -0.15) emotion = "negative";
            else emotion = "neutral";
        }

        // confidence: 命中越多越自信，3+ 已经比较确定
        double confidence = Math.min(1.0, (pos + neg) / 5.0);

        return EmotionResult.builder()
                .emotion(emotion)
                .score(Math.round(score * 1000) / 1000.0)
                .tags(hits)
                .confidence(Math.round(confidence * 100) / 100.0)
                .provider(name())
                .build();
    }

    private static int countOccurrences(String haystack, String needle) {
        if (needle.isEmpty()) return 0;
        int count = 0, idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
