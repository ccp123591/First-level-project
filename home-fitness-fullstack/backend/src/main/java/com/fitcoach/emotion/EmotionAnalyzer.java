package com.fitcoach.emotion;

/**
 * 情感分析器抽象。生产可注入多个实现（lexicon / mimo），由配置选择默认 bean。
 */
public interface EmotionAnalyzer {
    EmotionResult analyze(String text);
    String name();
}
