package com.fitcoach.emotion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单条情感记录的对外响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionResponse {
    private Long id;
    private String source;
    private Long refId;
    private String text;
    private String emotion;
    private Double score;
    private List<String> tags;
    private Double confidence;
    private String provider;
    private LocalDateTime createdAt;
}
