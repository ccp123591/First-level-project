package com.fitcoach.infra.memory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 高层接口：把 session/feedback/emotion 落到记忆里 & 检索 Top-K 写回 coach prompt。
 *
 * - addSessionMemory(uid, sid, text)：嵌入并存档；text 通常是 'YYYY-MM-DD action N 次 score=82, notes=...'
 * - recall(uid, query, k)：拼出 'Top-K 相关历史' 字符串供 prompt 注入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorMemoryService {

    @Value("${ai.memory.enabled:true}")
    private boolean enabled;

    @Value("${ai.memory.recall-top-k:3}")
    private int defaultTopK;

    private final VectorMemoryStore store;

    public boolean isEnabled() { return enabled; }

    public void addSessionMemory(Long userId, Long sessionId, String text) {
        if (!enabled) return;
        try {
            store.add(userId, "session", sessionId, text);
        } catch (Exception e) {
            log.warn("[memory] add 失败 user={} sid={} msg={}", userId, sessionId, e.getMessage());
        }
    }

    public void addFeedbackMemory(Long userId, Long feedbackId, String text) {
        if (!enabled) return;
        try {
            store.add(userId, "feedback", feedbackId, text);
        } catch (Exception ignored) {
        }
    }

    /** 陪伴聊天 — 用户消息 / AI 回复 都用这个落库。sourceId 为 null（聊天不绑 session）。 */
    public void addChatMemory(Long userId, String text) {
        if (!enabled) return;
        if (text == null || text.isBlank()) return;
        try {
            store.add(userId, "chat", null, text);
        } catch (Exception e) {
            log.warn("[memory] addChat 失败 user={} msg={}", userId, e.getMessage());
        }
    }

    /** 返回 Top-K 相关历史的简短文本拼接（用于注入 prompt）。 */
    public String recall(Long userId, String query, int topK) {
        if (!enabled) return "";
        int k = topK > 0 ? topK : defaultTopK;
        try {
            List<MemoryRecord> hits = store.search(userId, query, k);
            if (hits.isEmpty()) return "";
            return hits.stream()
                    .map(r -> "- [" + r.getSourceType() + "] " + r.getText())
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.warn("[memory] recall 失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 按时间近的「叙旧」拉取 — 拼成可读的最近聊天片段，旧的在上、新的在下。
     */
    public String recentChat(Long userId, int limit) {
        if (!enabled) return "";
        try {
            List<MemoryRecord> hits = store.recentByUser(userId, "chat", limit);
            if (hits.isEmpty()) return "";
            // 倒过来：最旧的在上、最新的在下，更像"时间线"
            return hits.stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return Long.compare(a.getId(), b.getId());
                        if (a.getCreatedAt() == null) return -1;
                        if (b.getCreatedAt() == null) return 1;
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                    })
                    .map(r -> "- " + r.getText())
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.warn("[memory] recentChat 失败: {}", e.getMessage());
            return "";
        }
    }

    /** 触发用户全量重建（admin 用）。 */
    public void rebuildForUser(Long userId, List<MemoryRecord> rebuilt) {
        store.clearForUser(userId);
        for (MemoryRecord r : rebuilt) {
            if (r.getText() == null) continue;
            store.add(userId, r.getSourceType(), r.getSourceId(), r.getText());
        }
    }

    public long count(Long userId) {
        return store.countForUser(userId);
    }
}
