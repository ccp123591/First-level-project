package com.fitcoach.infra.memory;

import java.util.List;

/**
 * 向量记忆存储抽象 —— 抽离接口后生产可换 RediSearch / Qdrant / Pinecone 实现。
 */
public interface VectorMemoryStore {

    MemoryRecord add(Long userId, String sourceType, Long sourceId, String text);

    /** 在该用户的记忆里做 KNN 检索（cosine sim）。 */
    List<MemoryRecord> search(Long userId, String query, int topK);

    long countForUser(Long userId);

    void clearForUser(Long userId);

    String name();
}
