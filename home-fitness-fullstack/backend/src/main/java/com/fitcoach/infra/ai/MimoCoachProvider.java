package com.fitcoach.infra.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 小米 MiMo（OpenAI 兼容） — chat/completions 调用 + strict-JSON 反解。
 * 仅在 ai.coach.provider=mimo 时装配，避免与 MockCoachProvider 同时注入。
 *
 * 优雅降级：api-key 未配置时不抛异常，启动期 WARN 提示切回 mock。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.coach.provider", havingValue = "mimo")
public class MimoCoachProvider implements AiCoachProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int maxTokens;
    private final double temperature;
    private final RestClient restClient;

    public MimoCoachProvider(
            @Value("${ai.coach.mimo.api-key:}") String apiKey,
            @Value("${ai.coach.mimo.base-url:https://api.xiaomimimo.com/v1}") String baseUrl,
            @Value("${ai.coach.mimo.model:mimo-v2-flash}") String model,
            @Value("${ai.coach.mimo.max-tokens:600}") int maxTokens,
            @Value("${ai.coach.mimo.temperature:0.7}") double temperature,
            RestTemplateBuilder builder) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        // 用 RestTemplateBuilder 构造底层 RestTemplate —— @RestClientTest 会替换其 ClientHttpRequestFactory 为 MockRestServiceServer。
        this.restClient = RestClient.builder(builder.build()).build();
    }

    @PostConstruct
    void validate() {
        if (apiKey.isBlank()) {
            log.warn("[MiMo] api-key 为空 — provider=mimo 已启用但调用将失败；建议改用 ai.coach.provider=mock");
        }
        log.info("[MiMo] coach provider initialized (model={}, baseUrl={})", model, baseUrl);
    }

    @Override
    public String name() {
        return "mimo";
    }

    @Override
    public CoachAiResponse feedback(CoachContext ctx) {
        return call(CoachPromptTemplates.SYSTEM_FEEDBACK, ctx);
    }

    @Override
    public CoachAiResponse suggestion(CoachContext ctx) {
        return call(CoachPromptTemplates.SYSTEM_SUGGESTION, ctx);
    }

    @Override
    public CoachAiResponse weeklyPlan(CoachContext ctx) {
        return call(CoachPromptTemplates.SYSTEM_WEEKLY_PLAN, ctx);
    }

    private CoachAiResponse call(String systemPrompt, CoachContext ctx) {
        Map<String, Object> body = buildBody(systemPrompt, ctx);
        String url = baseUrl.replaceAll("/+$", "") + "/chat/completions";

        try {
            JsonNode root = restClient.post()
                    .uri(url)
                    .headers(this::applyAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            return parseResponse(root);
        } catch (RestClientException e) {
            log.warn("[MiMo] HTTP error: {}", e.getMessage());
            throw new BusinessException(503, "AI coach unavailable");
        }
    }

    private Map<String, Object> buildBody(String systemPrompt, CoachContext ctx) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", CoachPromptTemplates.userPrompt(ctx))
        ));
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);
        body.put("response_format", Map.of("type", "json_object"));
        return body;
    }

    private void applyAuth(HttpHeaders h) {
        if (!apiKey.isBlank()) {
            h.setBearerAuth(apiKey);
        }
    }

    private CoachAiResponse parseResponse(JsonNode root) {
        if (root == null) {
            throw new BusinessException(503, "AI coach unavailable");
        }

        int tokens = root.path("usage").path("total_tokens").asInt(0);
        String content = root.path("choices").path(0).path("message").path("content").asText("");

        // 尝试把 content 解为 strict-JSON；失败则把整段 content 当 review 返回（降级）。
        try {
            JsonNode parsed = MAPPER.readTree(content);
            return CoachAiResponse.builder()
                    .review(parsed.path("review").asText(""))
                    .suggestion(parsed.path("suggestion").asText(""))
                    .encouragement(parsed.path("encouragement").asText(""))
                    .nextGoal(parsed.path("nextGoal").asText(""))
                    .provider(name())
                    .tokensUsed(tokens)
                    .build();
        } catch (Exception parseFail) {
            return CoachAiResponse.builder()
                    .review(content)
                    .suggestion("")
                    .encouragement("")
                    .nextGoal("")
                    .provider(name())
                    .tokensUsed(tokens)
                    .build();
        }
    }
}
