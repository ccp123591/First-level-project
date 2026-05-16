package com.fitcoach.emotion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "ai.coach.provider=mock",
        "spring.flyway.enabled=false",
        "management.health.redis.enabled=false"
})
@Transactional
class EmotionControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired ObjectMapper mapper;

    private String bearer;

    @BeforeEach
    void setup() {
        bearer = "Bearer " + jwtUtil.generateAccessToken(77L, "tester", "USER");
    }

    @Test
    void analyze_persists_and_returns_emotion() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "text", "今天太棒了，状态非常好，有进步！",
                "source", "note"));
        mvc.perform(post("/api/emotion/analyze")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.emotion").value("positive"))
                .andExpect(jsonPath("$.data.score").value(org.hamcrest.Matchers.greaterThan(0.0)))
                .andExpect(jsonPath("$.data.provider").value("lexicon"));
    }

    @Test
    void analyze_blank_text_rejected_with_400() throws Exception {
        mvc.perform(post("/api/emotion/analyze")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\",\"source\":\"note\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void summary_returns_dominant_emotion_after_inserts() throws Exception {
        // insert two positive + one negative to make positive dominant
        analyzeText("太棒了，开心，加油！");
        analyzeText("满意，有成就感，状态好！");
        analyzeText("好累，难受，想放弃。");

        mvc.perform(get("/api/emotion/summary?days=7")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days").value(7))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.positive").value(2))
                .andExpect(jsonPath("$.data.negative").value(1))
                .andExpect(jsonPath("$.data.dominantEmotion").value("positive"));
    }

    @Test
    void history_paginated() throws Exception {
        analyzeText("好开心");
        mvc.perform(get("/api/emotion/history?page=0&size=5")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].emotion").exists())
                .andExpect(jsonPath("$.data.page").value(0));
    }

    private void analyzeText(String text) throws Exception {
        String body = mapper.writeValueAsString(Map.of("text", text, "source", "note"));
        mvc.perform(post("/api/emotion/analyze")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
