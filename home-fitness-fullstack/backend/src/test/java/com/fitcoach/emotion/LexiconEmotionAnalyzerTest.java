package com.fitcoach.emotion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class LexiconEmotionAnalyzerTest {

    private final LexiconEmotionAnalyzer analyzer = new LexiconEmotionAnalyzer();

    @Test
    void positive_text_yields_positive_emotion_with_score_above_zero() {
        EmotionResult r = analyzer.analyze("今天训练完成得很棒，状态非常好，有进步！");
        assertThat(r.getEmotion()).isEqualTo("positive");
        assertThat(r.getScore()).isGreaterThan(0.0);
        assertThat(r.getTags()).isNotEmpty();
        assertThat(r.getProvider()).isEqualTo("lexicon");
    }

    @Test
    void negative_text_yields_negative_emotion_with_score_below_zero() {
        EmotionResult r = analyzer.analyze("好累，今天太难受了，根本坚持不下去，想放弃了。");
        assertThat(r.getEmotion()).isEqualTo("negative");
        assertThat(r.getScore()).isLessThan(0.0);
        assertThat(r.getTags()).isNotEmpty();
    }

    @Test
    void neutral_text_yields_neutral_emotion_score_zero() {
        EmotionResult r = analyzer.analyze("今天做了一组深蹲。");
        assertThat(r.getEmotion()).isEqualTo("neutral");
        assertThat(r.getScore()).isCloseTo(0.0, within(0.001));
    }

    @Test
    void empty_text_yields_neutral() {
        EmotionResult r = analyzer.analyze("");
        assertThat(r.getEmotion()).isEqualTo("neutral");
        assertThat(r.getScore()).isCloseTo(0.0, within(0.001));
        assertThat(r.getTags()).isEmpty();
    }

    @Test
    void null_text_treated_as_empty() {
        EmotionResult r = analyzer.analyze(null);
        assertThat(r.getEmotion()).isEqualTo("neutral");
    }

    @Test
    void english_positive_words_also_counted() {
        EmotionResult r = analyzer.analyze("great workout today, feeling strong and proud!");
        assertThat(r.getEmotion()).isEqualTo("positive");
        assertThat(r.getScore()).isPositive();
    }

    @Test
    void mixed_text_dominant_side_wins() {
        EmotionResult r = analyzer.analyze("虽然有点累，但是完成了目标，很有成就感，加油！");
        // 1 negative (累) vs 3 positive (完成 成就感 加油) → positive
        assertThat(r.getEmotion()).isEqualTo("positive");
    }

    @Test
    void confidence_increases_with_matched_terms() {
        EmotionResult few = analyzer.analyze("还行。");
        EmotionResult many = analyzer.analyze("太棒了，特别开心，超级满意，非常有成就感！");
        assertThat(many.getConfidence()).isGreaterThan(few.getConfidence());
    }
}
