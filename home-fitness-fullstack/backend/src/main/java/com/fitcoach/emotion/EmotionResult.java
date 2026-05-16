package com.fitcoach.emotion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 情感分析结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionResult {
    /** positive / neutral / negative */
    private String emotion;
    /** score ∈ [-1.0, 1.0] */
    private Double score;
    /** 命中关键词或 LLM 提取标签 */
    private List<String> tags;
    /** 置信度（0 ~ 1） */
    private Double confidence;
    /** 实现：lexicon / mimo */
    private String provider;
}
