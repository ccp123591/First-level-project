package com.fitcoach.infra.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 向量记忆单条 —— userId 隔离，sourceType 标识来源（session / feedback / note）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryRecord {
    private Long id;
    private Long userId;
    /** session / feedback / emotion / note */
    private String sourceType;
    private Long sourceId;
    private String text;
    private float[] vector;
    private LocalDateTime createdAt;
}
