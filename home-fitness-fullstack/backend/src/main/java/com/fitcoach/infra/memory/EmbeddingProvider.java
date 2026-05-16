package com.fitcoach.infra.memory;

/**
 * 文本向量化抽象。Mock 实现用确定性 hash；生产可接 OpenAI / 百度 / Cohere 等。
 */
public interface EmbeddingProvider {
    /** 维度（不同实现可能不同；Mock 默认 64）。 */
    int dimension();

    /** 把文本嵌入到固定维度向量；保证同输入同输出。 */
    float[] embed(String text);

    /** 实现名（mock / openai / mimo …）。 */
    String name();
}
