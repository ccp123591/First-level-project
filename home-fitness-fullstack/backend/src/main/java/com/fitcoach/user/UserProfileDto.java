package com.fitcoach.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户画像对外结构。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long userId;
    private String nickname;
    private String favoriteAction;
    private String favoriteActionLabel;
    private Map<String, Long> actionCounts;
    private Integer bestScore;
    private Integer avgScore;
    /** rhythm / stability / depth / symmetry / completion */
    private String weakestDimension;
    private Integer streakDays;
    private Integer totalDays;
    private Long totalReps;
    private List<String> recentNotes;
    private String summaryText;
    private String summarizer;
    private Integer version;
    private LocalDateTime updatedAt;
}
