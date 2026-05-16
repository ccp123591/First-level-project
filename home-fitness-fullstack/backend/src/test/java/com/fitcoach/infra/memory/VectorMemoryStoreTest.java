package com.fitcoach.infra.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class VectorMemoryStoreTest {

    private InMemoryVectorMemoryStore store;
    private MockEmbeddingProvider emb;

    @BeforeEach
    void setup() {
        emb = new MockEmbeddingProvider();
        store = new InMemoryVectorMemoryStore(emb);
        ReflectionTestUtils.setField(store, "maxPerUser", 100);
    }

    @Test
    void mock_embedding_is_deterministic_and_l2_normalized() {
        float[] a = emb.embed("hello world");
        float[] b = emb.embed("hello world");
        assertThat(a).containsExactly(b);

        double norm = 0;
        for (float f : a) norm += f * f;
        assertThat(Math.sqrt(norm)).isCloseTo(1.0, within(1e-5));
    }

    @Test
    void search_returns_self_match_first() {
        store.add(7L, "session", 1L, "2026-05-10 深蹲 20 次 得分 85");
        store.add(7L, "session", 2L, "2026-05-11 俯卧撑 15 次 得分 75");
        store.add(7L, "session", 3L, "2026-05-12 平板支撑 60 秒 得分 90");

        List<MemoryRecord> hits = store.search(7L, "2026-05-11 俯卧撑 15 次 得分 75", 1);
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getText()).contains("俯卧撑");
    }

    @Test
    void user_isolation_search_only_returns_own_records() {
        store.add(1L, "session", 1L, "alice squat");
        store.add(2L, "session", 1L, "bob pushup");
        var hits = store.search(1L, "anything", 10);
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getText()).isEqualTo("alice squat");
    }

    @Test
    void rolling_eviction_when_over_capacity() {
        ReflectionTestUtils.setField(store, "maxPerUser", 3);
        for (int i = 1; i <= 5; i++) store.add(7L, "session", (long) i, "rec-" + i);
        assertThat(store.countForUser(7L)).isEqualTo(3);
        // 应留下后 3 条
        var hits = store.search(7L, "rec-5", 3);
        assertThat(hits).extracting(MemoryRecord::getText).contains("rec-5", "rec-4", "rec-3");
    }

    @Test
    void empty_or_null_query_returns_empty() {
        store.add(7L, "session", 1L, "x");
        assertThat(store.search(7L, "", 5)).isEmpty();
        assertThat(store.search(7L, null, 5)).isEmpty();
    }

    @Test
    void cosine_of_same_vector_is_one() {
        float[] v = emb.embed("anything");
        assertThat(InMemoryVectorMemoryStore.cosine(v, v)).isCloseTo(1.0f, within(1e-4f));
    }
}
