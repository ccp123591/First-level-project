package com.fitcoach.infra.vision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * vision-svc 推理服务返回的房间结构化特征 — 与 Python sidecar 的 JSON schema 一致。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomFeatures {

    /** 估算可用面积（平米）；null 表示尺度估算失败。 */
    private Double areaSqm;
    /** 面积估算置信度 0..1 */
    private Double areaConfidence;
    /** living-room / bedroom / office / unknown */
    private String roomType;
    /** good / dim / poor */
    private String lighting;
    /** hardwood / carpet / tile / unknown */
    private String floor;

    private List<Obstacle> obstacles;

    private List<String> recommendedActions;
    private List<DiscouragedAction> discouragedActions;

    private Integer safetyScore;
    private List<String> warnings;

    /** 实际跑推理的模型标签（'placeholder' / 'midas-small+u2net' …）。 */
    private String model;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Obstacle {
        private String label;        // sofa / table / chair / bed / wall / unknown
        private int[] bbox;          // [x1,y1,x2,y2] in px
        private Double distanceM;
        private String side;         // left / right / front / behind
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscouragedAction {
        private String action;
        private String reason;
    }
}
