package com.fitcoach.emotion;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 单条情感分析记录。score ∈ [-1.0, 1.0]：负 → 负面，0 → 中性，正 → 积极。
 */
@Entity
@Table(name = "t_emotion_record",
    indexes = {
        @Index(name = "idx_emotion_user_created", columnList = "userId,createdAt"),
        @Index(name = "idx_emotion_source", columnList = "source,refId")
    })
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmotionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** 来源：note / post / chat / session / generic */
    @Column(nullable = false, length = 32)
    private String source;

    /** 关联的外部 id（可空） */
    private Long refId;

    /** 原始文本（最长 1000，超过截断）。 */
    @Column(length = 1000)
    private String text;

    /** positive / neutral / negative */
    @Column(nullable = false, length = 16)
    private String emotion;

    /** score in [-1.0, 1.0] */
    @Column(nullable = false)
    private Double score;

    /** 命中关键词列表（逗号分隔），方便审计。 */
    @Column(length = 256)
    private String tags;

    /** 由哪个 analyzer 输出（lexicon / mimo …） */
    @Column(length = 32)
    private String provider;

    @CreatedDate
    private LocalDateTime createdAt;
}
