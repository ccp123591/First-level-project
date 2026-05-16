package com.fitcoach.emotion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface EmotionRecordRepository extends JpaRepository<EmotionRecord, Long> {

    Page<EmotionRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** 取近 N 天的记录（按 createdAt >= since 过滤）。 */
    List<EmotionRecord> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime since);

    @Query("""
            select e.emotion as emotion, count(e) as cnt
            from EmotionRecord e
            where e.userId = :uid and e.createdAt >= :since
            group by e.emotion
            """)
    List<EmotionCountRow> countByEmotionSince(Long uid, LocalDateTime since);

    interface EmotionCountRow {
        String getEmotion();
        Long getCnt();
    }
}
