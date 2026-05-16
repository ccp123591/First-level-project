package com.fitcoach.room;

import com.fitcoach.infra.vision.RoomFeatures;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * /api/room/scan 与 /api/room/me 的响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomLayoutResponse {
    private Long id;
    private String scanId;
    private String summaryText;
    private BigDecimal areaSqm;
    private Integer safetyScore;
    private String visionModel;
    private LocalDateTime capturedAt;
    private LocalDateTime processedAt;
    /** 完整的 RoomFeatures（已反序列化），前端可拿来渲染。 */
    private RoomFeatures features;
}
