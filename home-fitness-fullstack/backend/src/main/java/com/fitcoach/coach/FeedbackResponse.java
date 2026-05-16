package com.fitcoach.coach;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 教练反馈对外响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {
    private Long id;
    private String review;
    private String suggestion;
    private String encouragement;
    private String nextGoal;
    private String provider;
    private Integer tokensUsed;
    private LocalDateTime createdAt;
}
