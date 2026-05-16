package com.fitcoach.coach;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.security.JwtUtil;
import com.fitcoach.session.Session;
import com.fitcoach.session.SessionRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "ai.coach.provider=mock",
        "spring.flyway.enabled=false",
        "management.health.redis.enabled=false"
})
@Transactional
class CoachControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired SessionRepository sessionRepo;
    @Autowired JwtUtil jwtUtil;
    @Autowired ObjectMapper mapper;

    private String bearer;
    private Long sessionId;

    @BeforeEach
    void setup() {
        // 用 JwtUtil 给一个固定 userId 签 token
        String token = jwtUtil.generateAccessToken(42L, "test", "USER");
        bearer = "Bearer " + token;

        Session s = Session.builder()
                .userId(42L)
                .action("squat").actionLabel("深蹲")
                .reps(15).score(80).duration(120)
                .sessionDate("2026-05-16")
                .build();
        sessionId = sessionRepo.save(s).getId();
    }

    @Test
    void feedback_returns_mock_provider_payload() throws Exception {
        String body = mapper.writeValueAsString(Map.of("sessionId", sessionId));
        mvc.perform(post("/api/coach/feedback")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.provider").value("mock"))
                .andExpect(jsonPath("$.data.review").exists())
                .andExpect(jsonPath("$.data.suggestion").exists());
    }

    @Test
    void history_returns_pageresult_shape() throws Exception {
        mvc.perform(get("/api/coach/history?page=0&size=5")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items").exists())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(5));
    }

    @Test
    void feedback_missing_sessionId_rejected_with_400() throws Exception {
        mvc.perform(post("/api/coach/feedback")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
