package com.fitcoach.coach;

import com.fitcoach.infra.ai.CoachAiResponse;
import com.fitcoach.infra.ai.CoachContext;
import com.fitcoach.infra.ai.MockCoachProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockCoachProviderTest {

    @Test
    void mock_provider_returns_deterministic_feedback_from_context() {
        var p = new MockCoachProvider();
        var ctx = CoachContext.builder()
                .action("squat").actionLabel("深蹲")
                .reps(15).score(82)
                .build();
        CoachAiResponse r = p.feedback(ctx);

        assertThat(r.getReview()).contains("深蹲").contains("15");
        assertThat(r.getProvider()).isEqualTo("mock");
        assertThat(r.getTokensUsed()).isZero();
        assertThat(r.getSuggestion()).isNotBlank();
        assertThat(r.getEncouragement()).isNotBlank();
        assertThat(r.getNextGoal()).isNotBlank();
    }

    @Test
    void mock_provider_suggestion_uses_recent_aggregates() {
        var p = new MockCoachProvider();
        var ctx = CoachContext.builder()
                .nickname("Tom")
                .recentAvgScore(78).recentTotalReps(120L)
                .build();
        CoachAiResponse r = p.suggestion(ctx);
        assertThat(r.getReview()).contains("78");
        assertThat(r.getProvider()).isEqualTo("mock");
    }

    @Test
    void mock_provider_weeklyPlan_returns_complete_response() {
        var p = new MockCoachProvider();
        CoachAiResponse r = p.weeklyPlan(CoachContext.builder().build());
        assertThat(r.getReview()).isNotBlank();
        assertThat(r.getSuggestion()).isNotBlank();
        assertThat(r.getProvider()).isEqualTo("mock");
        assertThat(r.getTokensUsed()).isZero();
    }

    @Test
    void mock_provider_name_is_mock() {
        assertThat(new MockCoachProvider().name()).isEqualTo("mock");
    }
}
