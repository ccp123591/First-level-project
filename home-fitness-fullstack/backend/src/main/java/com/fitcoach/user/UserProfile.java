package com.fitcoach.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 用户画像 — 由 ProfileExtractionService 周期性 / session 创建后刷新写入。
 */
@Entity
@Table(name = "t_user_profile")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String profileJson;

    @Column(length = 800)
    private String summaryText;

    @Builder.Default
    @Column(length = 16)
    private String summarizer = "aggregate";

    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
