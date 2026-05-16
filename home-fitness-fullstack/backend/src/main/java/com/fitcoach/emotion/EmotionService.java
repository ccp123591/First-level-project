package com.fitcoach.emotion;

import com.fitcoach.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 情感计算编排：调 analyzer → 持久化 → 历史 / 汇总查询。
 */
@Service
@RequiredArgsConstructor
public class EmotionService {

    private static final int MAX_TEXT = 1000;

    private final EmotionAnalyzer analyzer;
    private final EmotionRecordRepository repo;

    @Transactional
    public EmotionResponse analyze(Long userId, String source, Long refId, String text) {
        EmotionResult r = analyzer.analyze(text);

        EmotionRecord rec = new EmotionRecord();
        rec.setUserId(userId);
        rec.setSource(source == null || source.isBlank() ? "generic" : source);
        rec.setRefId(refId);
        rec.setText(truncate(text));
        rec.setEmotion(r.getEmotion());
        rec.setScore(r.getScore());
        rec.setTags(r.getTags() == null || r.getTags().isEmpty()
                ? null : String.join(",", r.getTags()));
        rec.setProvider(r.getProvider());
        rec = repo.save(rec);

        return toResponse(rec, r);
    }

    @Transactional(readOnly = true)
    public PageResult<EmotionResponse> history(Long userId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        Page<EmotionRecord> p = repo.findByUserIdOrderByCreatedAtDesc(userId,
                PageRequest.of(safePage, safeSize));
        List<EmotionResponse> items = p.getContent().stream().map(this::toResponse).toList();
        return PageResult.of(items, p.getTotalElements(), safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public EmotionSummary summary(Long userId, int days) {
        int safeDays = Math.max(1, Math.min(days, 90));
        LocalDateTime since = LocalDateTime.now().minusDays(safeDays);

        Map<String, Long> counts = new HashMap<>();
        for (var row : repo.countByEmotionSince(userId, since)) {
            counts.put(row.getEmotion(), row.getCnt());
        }
        long positive = counts.getOrDefault("positive", 0L);
        long neutral  = counts.getOrDefault("neutral",  0L);
        long negative = counts.getOrDefault("negative", 0L);
        long total    = positive + neutral + negative;

        Double avg = repo.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, since)
                .stream()
                .mapToDouble(EmotionRecord::getScore)
                .average().orElse(0.0);
        avg = Math.round(avg * 1000) / 1000.0;

        String dominant = total == 0 ? "neutral" :
                Arrays.stream(new String[]{"positive","neutral","negative"})
                        .max((a, b) -> Long.compare(counts.getOrDefault(a, 0L), counts.getOrDefault(b, 0L)))
                        .orElse("neutral");

        return EmotionSummary.builder()
                .days(safeDays)
                .total(total)
                .positive(positive)
                .neutral(neutral)
                .negative(negative)
                .avgScore(avg)
                .dominantEmotion(dominant)
                .build();
    }

    // ---- helpers ----

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() <= MAX_TEXT ? s : s.substring(0, MAX_TEXT);
    }

    private EmotionResponse toResponse(EmotionRecord rec, EmotionResult r) {
        return EmotionResponse.builder()
                .id(rec.getId())
                .source(rec.getSource())
                .refId(rec.getRefId())
                .text(rec.getText())
                .emotion(rec.getEmotion())
                .score(rec.getScore())
                .tags(r.getTags())
                .confidence(r.getConfidence())
                .provider(rec.getProvider())
                .createdAt(rec.getCreatedAt())
                .build();
    }

    private EmotionResponse toResponse(EmotionRecord rec) {
        return EmotionResponse.builder()
                .id(rec.getId())
                .source(rec.getSource())
                .refId(rec.getRefId())
                .text(rec.getText())
                .emotion(rec.getEmotion())
                .score(rec.getScore())
                .tags(rec.getTags() == null || rec.getTags().isBlank()
                        ? List.of() : Arrays.asList(rec.getTags().split(",")))
                .provider(rec.getProvider())
                .createdAt(rec.getCreatedAt())
                .build();
    }
}
