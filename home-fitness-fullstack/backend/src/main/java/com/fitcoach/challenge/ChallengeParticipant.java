package com.fitcoach.challenge;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 挑战参与者状态记录。
 */
@Entity
@Table(name = "t_challenge_participant",
    uniqueConstraints = @UniqueConstraint(name = "uk_chal_user", columnNames = {"challengeId", "userId"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long challengeId;

    @Column(nullable = false)
    private Long userId;

    private LocalDateTime joinedAt;

    @Builder.Default
    private Integer progressReps = 0;

    @Builder.Default
    private Boolean completed = false;
}
