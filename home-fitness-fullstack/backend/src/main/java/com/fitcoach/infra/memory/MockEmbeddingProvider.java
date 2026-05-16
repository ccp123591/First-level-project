package com.fitcoach.infra.memory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Mock embedding：把文本 SHA-256 之后切成 32 段，每段 8 字节归一化为 [-1,1] float，
 * 拼成 32 维向量并 L2 归一化。完全确定性，零外部依赖，适合单测与本地 RAG demo。
 *
 * 与真实 embedding 不同的是它没有"语义相似度" — 相似输入只在大量重复词时才接近。
 * 但接口和向量空间形态一致，便于切换。
 */
@Component
@ConditionalOnProperty(name = "ai.memory.embedding", havingValue = "mock", matchIfMissing = true)
public class MockEmbeddingProvider implements EmbeddingProvider {

    public static final int DIM = 32;

    @Override
    public int dimension() { return DIM; }

    @Override
    public String name() { return "mock"; }

    @Override
    public float[] embed(String text) {
        if (text == null) text = "";
        byte[] hash = sha256(text);
        // 取 32 字节，每字节作为一个维度，缩放到 [-1, 1]
        float[] v = new float[DIM];
        for (int i = 0; i < DIM; i++) {
            int b = hash[i] & 0xFF;          // 0..255
            v[i] = (b - 128) / 128.0f;       // → [-1, ~1)
        }
        return l2Normalize(v);
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static float[] l2Normalize(float[] v) {
        double sum = 0;
        for (float f : v) sum += f * f;
        double norm = Math.sqrt(sum);
        if (norm < 1e-9) return v;
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = (float) (v[i] / norm);
        return out;
    }
}
