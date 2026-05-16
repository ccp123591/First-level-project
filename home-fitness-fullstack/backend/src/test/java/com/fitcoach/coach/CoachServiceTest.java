package com.fitcoach.coach;

import com.fitcoach.exception.BusinessException;
import com.fitcoach.infra.ai.AiCoachProvider;
import com.fitcoach.infra.ai.CoachAiResponse;
import com.fitcoach.session.Session;
import com.fitcoach.session.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CoachServiceTest {

    @Mock SessionRepository sessionRepo;
    @Mock CoachFeedbackRepository fbRepo;
    @Mock AiCoachProvider provider;
    @Mock com.fitcoach.emotion.EmotionService emotionService;

    @InjectMocks CoachService service;

    private Session ownSession() {
        Session s = new Session();
        s.setId(1L);
        s.setUserId(7L);
        s.setAction("squat");
        s.setActionLabel("深蹲");
        s.setReps(15);
        s.setScore(80);
        s.setDuration(120);
        return s;
    }

    @Test
    void feedback_reads_session_and_persists_response() {
        Session s = ownSession();
        given(sessionRepo.findById(1L)).willReturn(Optional.of(s));
        given(sessionRepo.findByUserIdOrderBySessionDateDesc(7L)).willReturn(List.of(s));
        given(provider.feedback(any())).willReturn(
                CoachAiResponse.builder()
                        .review("r").suggestion("s").encouragement("e").nextGoal("g")
                        .provider("mock").tokensUsed(0).build());
        given(fbRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

        FeedbackResponse resp = service.feedback(7L, 1L);

        assertThat(resp.getReview()).isEqualTo("r");
        assertThat(resp.getProvider()).isEqualTo("mock");
        verify(fbRepo).save(argThat(f ->
                f.getUserId().equals(7L)
                        && f.getSessionId().equals(1L)
                        && "r".equals(f.getReview())
                        && "mock".equals(f.getProvider())));
    }

    @Test
    void feedback_403_when_session_belongs_to_other_user() {
        Session s = ownSession();
        s.setUserId(999L);
        given(sessionRepo.findById(1L)).willReturn(Optional.of(s));

        assertThatThrownBy(() -> service.feedback(7L, 1L))
                .isInstanceOf(BusinessException.class);
        verify(provider, never()).feedback(any());
        verify(fbRepo, never()).save(any());
    }

    @Test
    void feedback_404_when_session_missing() {
        given(sessionRepo.findById(42L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.feedback(7L, 42L))
                .isInstanceOf(BusinessException.class);
        verify(provider, never()).feedback(any());
    }

    @Test
    void suggestion_aggregates_recent_sessions_and_persists() {
        Session s1 = ownSession(); s1.setScore(80);
        Session s2 = ownSession(); s2.setScore(70); s2.setReps(10);
        given(sessionRepo.findByUserIdOrderBySessionDateDesc(7L)).willReturn(List.of(s1, s2));
        given(provider.suggestion(any())).willReturn(
                CoachAiResponse.builder()
                        .review("r2").suggestion("s2").encouragement("e2").nextGoal("g2")
                        .provider("mock").tokensUsed(0).build());
        given(fbRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

        FeedbackResponse resp = service.suggestion(7L);

        assertThat(resp.getReview()).isEqualTo("r2");
        verify(fbRepo).save(argThat(f -> f.getUserId().equals(7L) && f.getSessionId() == null));
    }

    @Test
    void history_returns_paginated_results() {
        CoachFeedback fb = new CoachFeedback();
        fb.setId(11L); fb.setUserId(7L); fb.setReview("hello");
        fb.setProvider("mock");

        given(fbRepo.findByUserIdOrderByCreatedAtDesc(7L, PageRequest.of(0, 10)))
                .willReturn(new PageImpl<>(List.of(fb), PageRequest.of(0, 10), 1));

        var page = service.history(7L, 0, 10);

        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getItems().get(0).getReview()).isEqualTo("hello");
    }
}
