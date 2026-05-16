package com.fitcoach.emotion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * N 天情感汇总（dashboard 用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionSummary {
    /** 统计窗口（天） */
    private int days;
    /** 命中样本数 */
    private long total;
    /** 各情感分类的计数 */
    private long positive;
    private long neutral;
    private long negative;
    /** 平均 score（-1..1） */
    private Double avgScore;
    /** 计数最多的情感 */
    private String dominantEmotion;
}
