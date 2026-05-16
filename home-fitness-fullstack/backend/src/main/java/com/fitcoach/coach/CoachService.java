package com.fitcoach.coach;

import com.fitcoach.common.PageResult;
import com.fitcoach.exception.BusinessException;
import com.fitcoach.infra.ai.AiCoachProvider;
import com.fitcoach.infra.ai.CoachAiResponse;
import com.fitcoach.infra.ai.CoachContext;
import com.fitcoach.session.Session;
import com.fitcoach.session.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI 教练编排：读取 session → 构造 CoachContext → 调 provider → 持久化 + 返回 FeedbackResponse。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoachService {

    private static final int RECENT_WINDOW = 7;

    private final SessionRepository sessionRepo;
    private final CoachFeedbackRepository fbRepo;
    private final AiCoachProvider provider;

    @Transactional
    public FeedbackResponse feedback(Long userId, Long sessionId) {
        Session s = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new BusinessException(404, "训练记录不存在"));
        if (!s.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该训练记录");
        }

        CoachContext ctx = buildContext(userId, s);
        CoachAiResponse ai = provider.feedback(ctx);

        CoachFeedback saved = persist(userId, sessionId, ai);
        return toResponse(saved, ai);
    }

    @Transactional
    public FeedbackResponse suggestion(Long userId) {
        CoachContext ctx = buildContext(userId, null);
        CoachAiResponse ai = provider.suggestion(ctx);
        CoachFeedback saved = persist(userId, null, ai);
        return toResponse(saved, ai);
    }

    @Transactional
    public FeedbackResponse weeklyPlan(Long userId) {
        CoachContext ctx = buildContext(userId, null);
        CoachAiResponse ai = provider.weeklyPlan(ctx);
        CoachFeedback saved = persist(userId, null, ai);
        return toResponse(saved, ai);
    }

    @Transactional(readOnly = true)
    public PageResult<FeedbackResponse> history(Long userId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        Page<CoachFeedback> p = fbRepo.findByUserIdOrderByCreatedAtDesc(userId,
                PageRequest.of(safePage, safeSize));
        List<FeedbackResponse> items = p.getContent().stream().map(this::toResponse).toList();
        return PageResult.of(items, p.getTotalElements(), safePage, safeSize);
    }

    // —— helpers ——

    private CoachContext buildContext(Long userId, Session current) {
        List<Session> recent = sessionRepo.findByUserIdOrderBySessionDateDesc(userId);
        List<Session> window = recent.size() > RECENT_WINDOW ? recent.subList(0, RECENT_WINDOW) : recent;

        long totalReps = window.stream().mapToInt(s -> s.getReps() == null ? 0 : s.getReps()).sum();
        int avgScore = window.isEmpty() ? 0 : (int) Math.round(
                window.stream().mapToInt(s -> s.getScore() == null ? 0 : s.getScore()).average().orElse(0));

        List<CoachContext.RecentSession> recentDtos = window.stream()
                .map(s -> CoachContext.RecentSession.builder()
                        .date(s.getSessionDate())
                        .action(s.getAction())
                        .reps(s.getReps())
                        .score(s.getScore())
                        .build())
                .toList();

        CoachContext.CoachContextBuilder b = CoachContext.builder()
                .userId(userId)
                .recentAvgScore(avgScore)
                .recentTotalReps(totalReps)
                .recentSessions(recentDtos);

        if (current != null) {
            b.action(current.getAction())
                    .actionLabel(current.getActionLabel())
                    .reps(current.getReps())
                    .targetReps(current.getTargetReps())
                    .duration(current.getDuration())
                    .score(current.getScore())
                    .rhythmScore(current.getRhythmScore())
                    .stabilityScore(current.getStabilityScore())
                    .depthScore(current.getDepthScore())
                    .symmetryScore(current.getSymmetryScore())
                    .completionScore(current.getCompletionScore());
        }
        return b.build();
    }

    private CoachFeedback persist(Long userId, Long sessionId, CoachAiResponse ai) {
        CoachFeedback fb = new CoachFeedback();
        fb.setUserId(userId);
        fb.setSessionId(sessionId);
        fb.setReview(ai.getReview());
        fb.setSuggestion(ai.getSuggestion());
        fb.setEncouragement(ai.getEncouragement());
        fb.setNextGoal(ai.getNextGoal());
        fb.setProvider(ai.getProvider() != null ? ai.getProvider() : provider.name());
        fb.setTokensUsed(ai.getTokensUsed() == null ? 0 : ai.getTokensUsed());
        return fbRepo.save(fb);
    }

    private FeedbackResponse toResponse(CoachFeedback fb, CoachAiResponse ai) {
        return FeedbackResponse.builder()
                .id(fb.getId())
                .review(ai.getReview())
                .suggestion(ai.getSuggestion())
                .encouragement(ai.getEncouragement())
                .nextGoal(ai.getNextGoal())
                .provider(fb.getProvider())
                .tokensUsed(fb.getTokensUsed())
                .createdAt(fb.getCreatedAt())
                .build();
    }

    private FeedbackResponse toResponse(CoachFeedback fb) {
        return FeedbackResponse.builder()
                .id(fb.getId())
                .review(fb.getReview())
                .suggestion(fb.getSuggestion())
                .encouragement(fb.getEncouragement())
                .nextGoal(fb.getNextGoal())
                .provider(fb.getProvider())
                .tokensUsed(fb.getTokensUsed())
                .createdAt(fb.getCreatedAt())
                .build();
    }
}
