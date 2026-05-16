package com.fitcoach.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.coach.CoachFeedback;
import com.fitcoach.coach.CoachFeedbackRepository;
import com.fitcoach.session.Session;
import com.fitcoach.session.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * 用户画像抽取：基于近 30 次 session + 反馈，聚合出动作偏好 / 强弱维度 / 总量 / 连续天数 / 简短中文摘要。
 * 默认确定性聚合（summarizer=aggregate）；后续可接入 MiMo 摘要器。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileExtractionService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int WINDOW = 30;

    private final SessionRepository sessionRepo;
    private final CoachFeedbackRepository feedbackRepo;
    private final UserProfileRepository profileRepo;
    private final UserRepository userRepo;

    /** 读取：缺失则现场聚合并落库。 */
    @Transactional
    public UserProfileDto read(Long userId) {
        return profileRepo.findByUserId(userId)
                .map(p -> {
                    UserProfileDto dto = aggregate(userId);
                    dto.setUpdatedAt(p.getUpdatedAt());
                    dto.setVersion(p.getVersion());
                    return dto;
                })
                .orElseGet(() -> refresh(userId));
    }

    /** 主动刷新并写入 t_user_profile。返回最新 DTO。 */
    @Transactional
    public UserProfileDto refresh(Long userId) {
        UserProfileDto dto = aggregate(userId);
        UserProfile p = profileRepo.findByUserId(userId).orElseGet(() -> {
            UserProfile fresh = new UserProfile();
            fresh.setUserId(userId);
            fresh.setVersion(0);
            return fresh;
        });
        p.setSummaryText(dto.getSummaryText());
        p.setSummarizer("aggregate");
        p.setVersion((p.getVersion() == null ? 0 : p.getVersion()) + 1);
        try {
            p.setProfileJson(MAPPER.writeValueAsString(dto));
        } catch (JsonProcessingException e) {
            log.warn("[profile] json 序列化失败，仅保留 summary: {}", e.getMessage());
        }
        profileRepo.save(p);
        dto.setVersion(p.getVersion());
        dto.setUpdatedAt(p.getUpdatedAt());
        dto.setSummarizer(p.getSummarizer());
        return dto;
    }

    /** 纯聚合 — 不写库，可被测试单独覆盖。 */
    public UserProfileDto aggregate(Long userId) {
        List<Session> all = sessionRepo.findByUserIdOrderBySessionDateDesc(userId);
        List<Session> window = all.size() > WINDOW ? all.subList(0, WINDOW) : all;

        User u = userRepo.findById(userId).orElse(null);
        String nickname = u == null ? null : u.getNickname();

        // 动作偏好（计数 + 累计 reps，count 优先，count 相同看 reps）
        Map<String, Long> counts = new LinkedHashMap<>();
        Map<String, Long> repsByAction = new LinkedHashMap<>();
        Map<String, String> labelByAction = new HashMap<>();
        for (Session s : window) {
            counts.merge(s.getAction(), 1L, Long::sum);
            repsByAction.merge(s.getAction(), (long)(s.getReps() == null ? 0 : s.getReps()), Long::sum);
            if (s.getActionLabel() != null) labelByAction.putIfAbsent(s.getAction(), s.getActionLabel());
        }
        String fav = counts.entrySet().stream()
                .max(Comparator.<Map.Entry<String,Long>>comparingLong(Map.Entry::getValue)
                        .thenComparingLong(e -> repsByAction.getOrDefault(e.getKey(), 0L)))
                .map(Map.Entry::getKey).orElse(null);
        String favLabel = fav == null ? null : labelByAction.getOrDefault(fav, defaultLabel(fav));

        // 总量
        long totalReps = window.stream().mapToLong(s -> s.getReps() == null ? 0 : s.getReps()).sum();
        int avgScore = (int) Math.round(window.stream()
                .mapToInt(s -> s.getScore() == null ? 0 : s.getScore())
                .average().orElse(0));
        int bestScore = window.stream()
                .mapToInt(s -> s.getScore() == null ? 0 : s.getScore())
                .max().orElse(0);

        // 最弱维度
        String weakest = weakestDimension(window);

        // 连续 / 总天数
        Set<String> dates = new TreeSet<>(Comparator.reverseOrder());
        for (Session s : window) if (s.getSessionDate() != null) dates.add(s.getSessionDate());
        int streak = computeStreak(dates);
        int totalDays = dates.size();

        // 最近 5 条反馈 review 摘要（去 null + 截断）
        List<String> recentNotes = feedbackRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(CoachFeedback::getReview)
                .filter(Objects::nonNull)
                .limit(5)
                .map(r -> r.length() > 60 ? r.substring(0, 60) + "…" : r)
                .toList();

        String summary = buildSummary(nickname, favLabel, totalReps, avgScore, weakest, streak);

        return UserProfileDto.builder()
                .userId(userId)
                .nickname(nickname)
                .favoriteAction(fav)
                .favoriteActionLabel(favLabel)
                .actionCounts(counts)
                .bestScore(bestScore)
                .avgScore(avgScore)
                .weakestDimension(weakest)
                .streakDays(streak)
                .totalDays(totalDays)
                .totalReps(totalReps)
                .recentNotes(recentNotes)
                .summaryText(summary)
                .summarizer("aggregate")
                .build();
    }

    // ---- helpers ----

    private static String defaultLabel(String action) {
        return switch (action == null ? "" : action) {
            case "squat" -> "深蹲";
            case "pushup" -> "俯卧撑";
            case "plank" -> "平板支撑";
            case "stretch" -> "前屈伸展";
            case "lunge" -> "弓步蹲";
            case "bridge" -> "臀桥";
            case "jumpingJack" -> "开合跳";
            default -> action == null ? "训练" : action;
        };
    }

    private static String weakestDimension(List<Session> ws) {
        if (ws.isEmpty()) return null;
        double rhythm = avg(ws, Session::getRhythmScore);
        double stability = avg(ws, Session::getStabilityScore);
        double depth = avg(ws, Session::getDepthScore);
        double symmetry = avg(ws, Session::getSymmetryScore);
        double completion = avg(ws, Session::getCompletionScore);
        Map<String, Double> dims = new LinkedHashMap<>();
        dims.put("rhythm", rhythm);
        dims.put("stability", stability);
        dims.put("depth", depth);
        dims.put("symmetry", symmetry);
        dims.put("completion", completion);
        // 过滤掉无数据维度（avg = 0 表示全部为 null）
        Map.Entry<String, Double> min = dims.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .min(Map.Entry.comparingByValue())
                .orElse(null);
        return min == null ? null : min.getKey();
    }

    private static double avg(List<Session> ws, java.util.function.ToIntFunction<Session> picker) {
        return ws.stream()
                .map(s -> {
                    Integer v = null;
                    try {
                        int n = picker.applyAsInt(s);
                        v = n;
                    } catch (NullPointerException ignored) {}
                    return v;
                })
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average().orElse(0);
    }

    /** 反向遍历日期集合（已逆序），从今天开始连续匹配。 */
    private static int computeStreak(Set<String> dates) {
        if (dates.isEmpty()) return 0;
        LocalDate today = LocalDate.now();
        int streak = 0;
        for (int i = 0; i < 365; i++) {
            String d = today.minusDays(i).toString();
            if (dates.contains(d)) streak++;
            else if (i > 0) break;  // 今天没训练也算从昨天起步
        }
        return streak;
    }

    private static String buildSummary(String nickname, String favLabel, long totalReps,
                                       int avgScore, String weakest, int streak) {
        StringBuilder sb = new StringBuilder();
        if (nickname != null && !nickname.isBlank()) sb.append(nickname).append(" · ");
        if (favLabel != null) sb.append("偏爱 ").append(favLabel);
        if (totalReps > 0) sb.append("（近期累计 ").append(totalReps).append(" 次）");
        sb.append("，平均分 ").append(avgScore);
        if (weakest != null) sb.append("，").append(zhDim(weakest)).append("维度待提升");
        if (streak > 0) sb.append("，已连续训练 ").append(streak).append(" 天");
        return sb.toString();
    }

    private static String zhDim(String dim) {
        return switch (dim) {
            case "rhythm" -> "节奏";
            case "stability" -> "稳定性";
            case "depth" -> "深度";
            case "symmetry" -> "对称";
            case "completion" -> "完成度";
            default -> dim;
        };
    }
}
