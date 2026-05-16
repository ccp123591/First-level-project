package com.fitcoach.room;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_room_layout_snapshot",
    indexes = @Index(name = "idx_room_user_captured", columnList = "userId,capturedAt"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomLayoutSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 40)
    private String scanId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String featuresJson;

    @Column(nullable = false, length = 400)
    private String summaryText;

    @Column(precision = 5, scale = 2)
    private BigDecimal areaSqm;

    private Integer safetyScore;

    @Builder.Default
    @Column(length = 16)
    private String source = "camera";

    @Column(length = 32)
    private String visionModel;

    @Column(nullable = false)
    private LocalDateTime capturedAt;

    @Column(nullable = false)
    private LocalDateTime processedAt;
}
