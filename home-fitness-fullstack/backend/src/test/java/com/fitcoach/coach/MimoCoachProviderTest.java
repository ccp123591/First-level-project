package com.fitcoach.coach;

import com.fitcoach.infra.ai.CoachAiResponse;
import com.fitcoach.infra.ai.CoachContext;
import com.fitcoach.infra.ai.MimoCoachProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(MimoCoachProvider.class)
@TestPropertySource(properties = {
        "ai.coach.provider=mimo",
        "ai.coach.mimo.api-key=test-key",
        "ai.coach.mimo.base-url=https://api.xiaomimimo.com/v1",
        "ai.coach.mimo.model=mimo-v2-flash",
        "ai.coach.mimo.max-tokens=600",
        "ai.coach.mimo.temperature=0.7",
        "ai.coach.mimo.timeout-seconds=20"
})
class MimoCoachProviderTest {

    @Autowired
    MimoCoachProvider provider;

    @Autowired
    MockRestServiceServer server;

    @Test
    void parses_chat_completion_strict_json_response() {
        String body = """
                {"choices":[{"message":{"content":"{\\"review\\":\\"good\\",\\"suggestion\\":\\"x\\",\\"encouragement\\":\\"y\\",\\"nextGoal\\":\\"z\\"}"}}],"usage":{"total_tokens":120}}
                """;
        server.expect(requestTo("https://api.xiaomimimo.com/v1/chat/completions"))
                .andExpect(method(POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        CoachAiResponse r = provider.feedback(
                CoachContext.builder().action("squat").reps(10).build());

        assertThat(r.getReview()).isEqualTo("good");
        assertThat(r.getSuggestion()).isEqualTo("x");
        assertThat(r.getEncouragement()).isEqualTo("y");
        assertThat(r.getNextGoal()).isEqualTo("z");
        assertThat(r.getTokensUsed()).isEqualTo(120);
        assertThat(r.getProvider()).isEqualTo("mimo");
    }

    @Test
    void wraps_non_json_content_in_review_field() {
        String body = """
                {"choices":[{"message":{"content":"plain prose, not json"}}],"usage":{"total_tokens":33}}
                """;
        server.expect(requestTo("https://api.xiaomimimo.com/v1/chat/completions"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        CoachAiResponse r = provider.suggestion(CoachContext.builder().build());

        assertThat(r.getReview()).isEqualTo("plain prose, not json");
        assertThat(r.getProvider()).isEqualTo("mimo");
        assertThat(r.getTokensUsed()).isEqualTo(33);
    }

    @Test
    void translates_http_5xx_to_business_exception() {
        server.expect(requestTo("https://api.xiaomimimo.com/v1/chat/completions"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> provider.feedback(CoachContext.builder().build()))
                .isInstanceOf(com.fitcoach.exception.BusinessException.class)
                .hasMessageContaining("AI coach unavailable");
    }
}
