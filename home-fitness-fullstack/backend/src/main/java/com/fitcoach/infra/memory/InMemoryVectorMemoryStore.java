package com.fitcoach.infra.memory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存向量记忆 —— 按 userId 隔离，cosine similarity KNN。
 *
 * 适用场景：dev / 小规模 prod，单实例。生产可替换为 RediSearch / Qdrant / Pinecone
 * 实现（同样实现 VectorMemoryStore 接口）。
 *
 * 通过 ai.memory.store=memory 启用（默认）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.memory.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryVectorMemoryStore implements VectorMemoryStore {

    @Value("${ai.memory.max-per-user:500}")
    private int maxPerUser;

    private final EmbeddingProvider embeddingProvider;

    private final Map<Long, List<MemoryRecord>> byUser = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);

    @Override
    public MemoryRecord add(Long userId, String sourceType, Long sourceId, String text) {
        if (text == null || text.isBlank()) return null;
        float[] vec = embeddingProvider.embed(text);
        MemoryRecord rec = MemoryRecord.builder()
                .id(idSeq.getAndIncrement())
                .userId(userId).sourceType(sourceType).sourceId(sourceId)
                .text(text).vector(vec).createdAt(LocalDateTime.now())
                .build();
        List<MemoryRecord> list = byUser.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            list.add(rec);
            // 老化：超出 maxPerUser 时丢最早的
            while (list.size() > maxPerUser) list.remove(0);
        }
        log.debug("[memory] +1 user={} src={} total={} dim={}", userId, sourceType, list.size(), vec.length);
        return rec;
    }

    @Override
    public List<MemoryRecord> search(Long userId, String query, int topK) {
        if (query == null || query.isBlank()) return List.of();
        List<MemoryRecord> list = byUser.get(userId);
        if (list == null || list.isEmpty()) return List.of();
        float[] q = embeddingProvider.embed(query);
        // 取前 topK 按 cosine 相似度倒序
        List<MemoryRecord> snapshot;
        synchronized (list) { snapshot = new ArrayList<>(list); }
        snapshot.sort((a, b) -> Float.compare(cosine(b.getVector(), q), cosine(a.getVector(), q)));
        int k = Math.max(1, Math.min(topK, snapshot.size()));
        return snapshot.subList(0, k);
    }

    @Override
    public long countForUser(Long userId) {
        List<MemoryRecord> list = byUser.get(userId);
        return list == null ? 0L : list.size();
    }

    @Override
    public List<MemoryRecord> recentByUser(Long userId, String sourceType, int limit) {
        List<MemoryRecord> list = byUser.get(userId);
        if (list == null || list.isEmpty()) return List.of();
        List<MemoryRecord> snapshot;
        synchronized (list) { snapshot = new ArrayList<>(list); }
        if (sourceType != null) {
            snapshot.removeIf(r -> !sourceType.equals(r.getSourceType()));
        }
        // createdAt 可能为 null（极少数旧数据） — 按入库顺序（list 本身已是时间序）也能用
        snapshot.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return Long.compare(b.getId(), a.getId());
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        int k = Math.max(1, Math.min(limit, snapshot.size()));
        return snapshot.subList(0, k);
    }

    @Override
    public void clearForUser(Long userId) {
        byUser.remove(userId);
    }

    @Override
    public String name() {
        return "in-memory";
    }

    /** 都是 L2 归一化向量 → cosine 等于点积。 */
    public static float cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return -1f;
        float dot = 0;
        for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        return dot;
    }
}
