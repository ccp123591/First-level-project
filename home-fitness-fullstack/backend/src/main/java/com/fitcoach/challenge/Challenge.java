package com.fitcoach.challenge;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 挑战赛 — 一段时间内累计完成某动作 target_reps 次。
 */
@Entity
@Table(name = "t_challenge")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String title;

    @Column(length = 256)
    private String description;

    @Column(nullable = false, length = 32)
    private String action;

    @Column(nullable = false)
    private Integer targetReps;

    @Column(length = 32)
    private String startDate;

    @Column(nullable = false, length = 32)
    private String endDate;

    /** ACTIVE / ENDED */
    @Builder.Default
    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(length = 128)
    private String cover;

    @CreatedDate
    private LocalDateTime createdAt;
}
