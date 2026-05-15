# FitCoach Backend Completion — Design

- **Date**: 2026-05-13
- **Repo**: `home-fitness-fullstack/backend` (Spring Boot 3.2, Java 17)
- **Status**: Approved by user 2026-05-13
- **Author**: pair (Claude Scholar + user)

## 1. Goal

Turn the FitCoach Spring Boot backend from "competition demo with many mock/placeholder endpoints" into a single-host **production-ready** backend that fulfills every contract the Vue PWA frontend already consumes, with proper architecture (Service layer + DTOs), real AI integration (Xiaomi MiMo), Redis-backed security/rate-limit, Flyway migrations, observability, and tests.

The frontend `src/api/*.js` files are the binding contract — they MUST keep working without changes.

## 2. Scope

### 2.1 In scope (S1 – S13)

| # | Theme | Summary |
|---|-------|---------|
| S1 | Static resources + upload | Serve `/uploads/**`, raise multipart limit, MIME/extension whitelist (Apache Tika), 5MB cap |
| S2 | AI Coach (MiMo) | Remove Claude placeholder. Add `AiCoachProvider` interface + `MimoCoachProvider` (OpenAI-compatible) + `MockCoachProvider`. `feedback` reads session, persists to `t_coach_feedback`. `history` queries the table. `suggestion` and `weeklyPlan` aggregate the user's recent sessions and call the provider. |
| S3 | Challenge module | New `challenge/` package: `t_challenge`, `t_challenge_participant`, `ChallengeService`, `ChallengeController`. `list/join/rank/detail` all DB-backed. |
| S4 | Auth completion | Drop WeChat endpoint. Real SMS code (via `SmsSender` interface, default mock-logger) stored in Redis. Phone login. Forgot/reset password. Refresh-token jti blacklist (Redis). Email-code endpoint same pattern. |
| S5 | Service-layer fill-in | Add `PlanService`, `SocialService`, `LeaderboardService`, `AdminService`, `ExerciseService`, `CoachService`. Controllers become thin (request mapping + DTO mapping only). |
| S6 | DTOs + Bean Validation | Replace `Map<String, Object>` request/response bodies with `*Request` / `*Response` classes. Apply `@Valid`, `@NotBlank`, `@Email`, `@Size`, `@Min`, `@Max`. Two intentional Map cases retained: arbitrary update payloads for `PUT /users/me` and `PUT /sessions/{id}` (kept Map because frontend sends partial JSON). |
| S7 | Exception coverage | Add handlers for `MaxUploadSizeExceededException`, `NoHandlerFoundException`, `HttpRequestMethodNotSupportedException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException`. Set `spring.mvc.throw-exception-if-no-handler-found=true` + `spring.web.resources.add-mappings=false`. |
| S8 | Rate limit + request log | Redis token-bucket `RateLimitFilter`. Strict bucket on `/api/auth/**` (5 req/min per IP, 10/min per email). Lenient bucket on other authenticated endpoints (60/min/user). Access log filter writes `method path status duration userId requestId` with MDC. |
| S9 | DB migrations | Add Flyway (`flyway-core` + `flyway-mysql`). `db/migration/V1__init_schema.sql` reproduces current entities; `V2..V5` seed exercises/plans/badges/challenges. **prod**: Flyway migrate + `ddl-auto: validate`. **dev (H2)**: Flyway disabled, keep `ddl-auto: update` + `data.sql`. |
| S10 | Actuator + OpenAPI | Add `spring-boot-starter-actuator`. Expose `health, info, metrics`. Move OpenAPI annotations off `FitCoachApplication` into `OpenApiConfig`. Add `/actuator/health` as the docker healthcheck target. |
| S11 | Seed data fix | The 4 seeded official plans currently have `items_json='[]'`. Populate each with a real day-by-day item array. Add example challenges. |
| S12 | Tests | JUnit 5 + Mockito + MockMvc + `@DataJpaTest`. Targets: Service line coverage ≥ 70%, overall ≥ 55%. At least one happy-path + one 401/403/400 per controller. One end-to-end `FitnessFlowTest` covering register → login → create session → coach feedback → leaderboard. |
| S13 | Prod config hardening | `application-prod.yml`: JWT_SECRET required from env (fail-fast if <32 bytes), `server.error.include-message: never`, HSTS + nosniff + Referrer-Policy + Permissions-Policy headers via `HeadersConfigurer`. CORS origins from env-comma-list. |
| S14 | **User Profile + AI Memory** (added 2026-05-13) | New `t_user_profile` table. `ProfileExtractionService` aggregates favorite action, weakest dimension, streak, total reps, recent notes into a structured DTO + 1-sentence Chinese `summaryText`. v1 deterministic; v2 optional MiMo-summarized (`ai.profile.summarizer=mimo`, cached 1 day). Refreshed async via `@TransactionalEventListener` on session-create. `summaryText` is injected as user-profile context into every MiMo coach prompt, making suggestions personalized and continuous across sessions. New endpoints: `GET /api/users/me/profile`, `POST /api/users/me/profile/refresh`. |
| S15 | **Vector Memory (RAG over training history)** (added 2026-05-15) | Upgrade `redis:7-alpine` → `redis/redis-stack-server:7.4.0-v0` to gain RediSearch + vector indexes (FLAT/HNSW). New `infra/ai/embedding/` package: `EmbeddingProvider` interface + `MockEmbeddingProvider` (deterministic SHA-256→float seed, 1024-d) + `OpenAiCompatEmbeddingProvider` (uses MiMo's OpenAI-compatible `/embeddings` endpoint when configured). New `infra/ai/memory/VectorMemoryService` wrapping RediSearch `FT.SEARCH` KNN. On the same `SessionCreatedEvent` from S14, listener also embeds `"<actionLabel> <repsx> 节奏=R 稳定=S 深度=D 对称=Y 完成=C; <notes>"` into Redis index `idx:session:vec` with payload `{userId, sessionId, action, score, createdAt}`. `CoachService.suggestion`/`weeklyPlan`/`feedback` build a query vector from `(profile.summaryText + currentRequest)`, call `VectorMemoryService.searchSimilar(userId, qVec, K=5)`, and inject the retrieved snippets as a `recentMemories` block in the MiMo prompt. New endpoint `POST /api/users/me/memory/rebuild` performs full re-embed of the user's sessions (admin or self). Failure mode: any embedding/search exception → log WARN, skip RAG, fall back to S14-only path (RAG is enrichment, not the critical path). |

### 2.2 Out of scope

- MinIO/S3 object storage (interface only; local-disk impl retained)
- Real WeChat OAuth (endpoint removed entirely)
- Real SMS gateway (interface + mock-logger; documented TODO)
- Real email gateway (interface + mock-logger)
- Multi-node deployment / Kubernetes / load balancing
- Full-text search / Elasticsearch
- WebSocket / push notifications
- Migration of `Session.sessionDate` from `String` to `LocalDate` (keeps frontend wire-compat)

## 3. Architecture

### 3.1 Package layout

```
com.fitcoach/
├── FitCoachApplication.java
├── common/                # ApiResult, PageResult, Constants
├── config/                # OpenApiConfig, CorsConfig, JpaConfig, RedisConfig,
│                          # WebMvcConfig, FlywayConfig (profile-aware)
├── security/              # SecurityConfig, JwtUtil, JwtAuthFilter, SecurityUtil,
│                          # LoginAttemptService, RateLimitFilter, PasswordPolicy
├── exception/             # BusinessException, GlobalExceptionHandler (extended)
├── infra/                 # NEW – framework-agnostic infrastructure
│   ├── storage/           #   StorageService + LocalStorageService
│   ├── ai/                #   AiCoachProvider + MimoCoachProvider + MockCoachProvider
│   │                      #   + CoachPrompt, CoachResponse DTOs
│   ├── sms/               #   SmsSender + MockSmsSender
│   ├── mail/              #   MailSender + MockMailSender
│   └── ratelimit/         #   RateLimiter (Redis token bucket)
├── auth/                  # AuthController + AuthService + VerifyCodeService +
│                          # PasswordResetService + DTOs
├── user/                  # existing + DTOs
├── session/               # existing + DTOs
├── coach/                 # CoachController + CoachService (NEW) + entity + repo + DTOs
├── plan/                  # existing + PlanService (NEW) + DTOs
├── badge/                 # existing + DTOs
├── leaderboard/           # existing + LeaderboardService (NEW, Redis-cached)
├── social/                # existing + SocialService (NEW) + DTOs
├── challenge/             # NEW – Challenge, ChallengeParticipant, repos,
│                          # ChallengeService, ChallengeController, DTOs
├── exercise/              # existing + ExerciseService (NEW) + DTOs
└── admin/                 # existing + AdminService (NEW) + DTOs
```

### 3.2 New persistence tables

| Table | Purpose | Backed by Redis? |
|-------|---------|------------------|
| `t_refresh_token_blacklist` | Revoked refresh token jti (audit) | Yes (Redis primary, DB fallback) |
| `t_verify_code` | Verification-code audit log (sent/verified/expired) | Active codes in Redis, audit in DB |
| `t_password_reset` | Reset tokens with expiry | Yes (Redis primary) |
| `t_challenge` | Challenge definitions (admin-managed) | No |
| `t_challenge_participant` | Participation rows (one per user×challenge) | No |
| `t_login_attempt` | Failed-login audit (IP, email, ts) | No (Redis-only counter; DB audit on lockout) |

### 3.3 Redis key conventions

| Prefix | Key | TTL | Purpose |
|--------|-----|-----|---------|
| `verify:` | `sms:<phone>` / `email:<email>` | 5 min | Active verification code |
| `verify:sent:` | `<phone>` | 60 s | "send code" cooldown |
| `pwreset:` | `<token>` | 30 min | Password reset token → userId |
| `rt:black:` | `<jti>` | until token expiry | Revoked refresh token |
| `ratelimit:` | `<scope>:<id>` | 60 s sliding | Token-bucket counter |
| `login:fail:` | `<email>` / `ip:<ip>` | 15 min | Failed-login counter |
| `cache:lb:` | `weekly` / `monthly` / `friends:<userId>` | 60 s | Leaderboard cache |
| `cache:coach:` | `suggestion:<userId>` / `weekly:<userId>` | 5 min / 1 day | MiMo response cache |

### 3.4 API contract changes (frontend unchanged)

| Endpoint | Before | After |
|----------|--------|-------|
| `POST /api/coach/feedback` | hard-coded mock | reads sessionId, validates ownership, builds prompt from session + history, calls `AiCoachProvider`, persists `CoachFeedback`, returns parsed response |
| `GET /api/coach/suggestion` | hard-coded mock | aggregates last 7 sessions, calls provider, cached 5 min |
| `GET /api/coach/weekly-plan` | hard-coded mock | calls provider with weekly stats, cached 1 day |
| `GET /api/coach/history` | returns `[]` | paginated query of `t_coach_feedback` by userId desc |
| `GET /api/challenges` | 2 hard-coded items | `SELECT * FROM t_challenge WHERE status='ACTIVE'` |
| `POST /api/challenges/{id}/join` | no-op | inserts `t_challenge_participant`; idempotent |
| `GET /api/challenges/{id}/rank` | `[]` | aggregates participants' qualifying sessions, ranks by total reps |
| `POST /auth/login/email` | OK | adds rate limit + failed-attempt lockout |
| `POST /auth/sms/send` | 501 | generates 6-digit code, stores in Redis (`verify:sms:<phone>`, TTL 5min), invokes `SmsSender` (mock logs to console) |
| `POST /auth/login/phone` | 501 | validates code from Redis, find-or-create user by phone, issues tokens |
| `POST /auth/email/send` | 501 | same pattern with `MailSender` |
| `POST /auth/password/forgot` | absent | sends reset email with token, stores `pwreset:<token>` → userId |
| `POST /auth/password/reset` | absent | consumes token, updates password |
| `POST /auth/logout` | client-only | adds refresh-token jti to `rt:black:` until its expiry; also writes audit row |
| `POST /auth/login/wechat` | 501 stub | **REMOVED** (route and DTO deleted) |
| `GET /uploads/**` | 404 | served by `ResourceHandler` mapped from `${upload.dir}` |

New endpoints:
- `POST /api/auth/password/forgot`
- `POST /api/auth/password/reset`
- `POST /api/auth/sms/send` (was 501, now real)
- `POST /api/auth/email/send` (was 501, now real)
- `POST /api/auth/login/phone` (was 501, now real)
- `GET /api/challenges/{id}` (detail)
- `GET /actuator/health`, `GET /actuator/info`, `GET /actuator/metrics`

## 4. AI Coach Provider (Xiaomi MiMo)

### 4.1 Configuration

```yaml
ai:
  coach:
    enabled: true
    provider: ${AI_COACH_PROVIDER:mock}   # mock | mimo
    mimo:
      api-key: ${MIMO_API_KEY:}
      base-url: ${MIMO_BASE_URL:https://api.xiaomimimo.com/v1}
      model: ${MIMO_MODEL:mimo-v2-flash}
      max-tokens: 600
      temperature: 0.7
      timeout-seconds: 20
```

### 4.2 Provider interface

```java
package com.fitcoach.infra.ai;

public interface AiCoachProvider {
    CoachResponse feedback(CoachContext ctx);
    CoachResponse suggestion(CoachContext ctx);
    CoachResponse weeklyPlan(CoachContext ctx);
}
```

`CoachContext` carries: nickname, recent sessions summary, latest score breakdown. `CoachResponse` carries: `review`, `suggestion`, `encouragement`, `nextGoal`, `provider`, `tokensUsed`.

### 4.3 Bean selection

`@ConditionalOnProperty(name="ai.coach.provider", havingValue="mimo")` → `MimoCoachProvider`. Falls back to `MockCoachProvider` (always available, no key required). On startup, if `provider=mimo` but `api-key` is blank, log a warning and degrade to mock.

### 4.4 MiMo request

POST `${base-url}/chat/completions`, OpenAI-compatible:
```json
{
  "model": "mimo-v2-flash",
  "messages": [
    {"role": "system", "content": "<FitCoach system prompt; instructs strict JSON output>"},
    {"role": "user", "content": "<context as compact JSON>"}
  ],
  "max_tokens": 600,
  "temperature": 0.7,
  "response_format": {"type": "json_object"}
}
```

System prompt locks the output schema:
```json
{"review":"...","suggestion":"...","encouragement":"...","nextGoal":"..."}
```

`thinking` mode is **disabled** for feedback (latency / cost reasons). Parse `choices[0].message.content` as JSON; on parse failure, fall back to wrapping the raw string in `review`.

Spring 6 `RestClient` is used (not `WebClient`, no reactive stack needed). Timeout 20s. Errors mapped to `BusinessException(503, "AI coach temporarily unavailable")`.

## 5. Security hardening

- **Password policy**: ≥ 8 chars, at least 1 letter + 1 digit. Enforced at register, change-password, reset. Pure regex helper.
- **bcrypt**: cost 10 (Spring default).
- **JWT secret**: prod fail-fast if missing or `< 32` bytes. Centralized in `JwtUtil.@PostConstruct`.
- **Refresh-token blacklist**: jti claim added when generating refresh tokens. `JwtAuthFilter` (and refresh endpoint) check Redis; if present, reject.
- **Failed-login lockout**: 5 consecutive failures within 15 min locks the email (returns `423 Locked`). Counter in Redis. **Audit semantics**: `t_login_attempt` is written *only at the lockout boundary* (the 5th failure that triggers the lock and on the unlock event), not on every failed attempt — Redis carries the running counter.
- **Rate limit**:
  - `/api/auth/login/**` and `/api/auth/*/send`: 5 req/min/IP, 10/min/identifier
  - `/api/auth/register`: 3 req/min/IP
  - other `/api/**` authenticated: 60 req/min/user
  - exceeded → `HTTP 429` + `Retry-After` header
- **Upload**: `MultipartConfig` cap `5MB`, extension whitelist `[jpg, jpeg, png, webp]`, real MIME check via Tika, randomized filename, never trust client filename.
- **Headers (prod)**: HSTS (1 year), `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy: camera=(self), microphone=()`. CSP not added (would break Swagger UI).
- **Error responses (prod)**: `server.error.include-message: never`, `include-stacktrace: never`. Dev keeps verbose for debugging.
- **CORS (prod)**: origins from env `CORS_ALLOWED_ORIGINS` (comma-separated), no `*`.

## 6. Observability

- **Request log filter** (`RequestLoggingFilter`): assigns/propagates `X-Request-Id`, writes MDC `requestId` + `userId`, logs one line per request: `[<requestId>] <method> <path> status=<n> duration=<ms>ms user=<id|anon>`. Skips `/actuator/**`, `/swagger-ui/**`, `/uploads/**`.
- **Actuator**: `management.endpoints.web.exposure.include=health,info,metrics`. `info` includes git commit (via `git-commit-id-plugin`, optional — skip if maven plugin not added).
- **Logback (prod)**: rolling file `logs/fitcoach-%d.log`, 100MB × 30 files, console JSON optional.
- **MiMo cost telemetry**: every successful call increments `t_coach_feedback.tokens_used` and emits an INFO log line for offline aggregation.

## 7. Testing

| Layer | Tool | Scope |
|-------|------|-------|
| Unit | JUnit5 + Mockito | All services + JwtUtil + PasswordPolicy + RateLimiter + Tika-based UploadValidator |
| Repository | `@DataJpaTest` | Custom queries: `SessionRepository.aggregateSince`, `search`, `countByAction`, `findByUserIdSince` |
| Web slice | `@WebMvcTest` | Auth controller (login flow + validation) |
| Integration | `@SpringBootTest` + MockMvc | Each controller: 1 happy path + 1 401/403/400 |
| End-to-end | `@SpringBootTest` `FitnessFlowTest` | register → login → create session → coach feedback (mock provider) → leaderboard → badge check |

Coverage gates (advisory, not enforced in CI): Service ≥ 70% lines, project ≥ 55% lines.

Test profile (`application-test.yml`): H2 in-memory + embedded Redis (`it.ozimov:embedded-redis`) OR Testcontainers Redis. `application-test.yml` is added.

## 8. Database migration strategy

- New deps: `org.flywaydb:flyway-core`, `org.flywaydb:flyway-mysql` (prod runtime).
- Layout:
  ```
  src/main/resources/db/migration/
    V1__init_schema.sql        # all existing tables + 6 new
    V2__seed_exercises.sql
    V3__seed_plans.sql         # plans with REAL itemsJson, not '[]'
    V4__seed_badges.sql
    V5__seed_challenges.sql    # example challenges
  ```
- Profile behaviour:
  - **dev (H2)**: `spring.flyway.enabled=false`, `ddl-auto=update`, `data.sql` still loaded.
  - **prod (MySQL)**: `spring.flyway.enabled=true`, `ddl-auto=validate`. Migrations run on startup.
- Rationale: the dev flow is "start backend, instantly playable"; Flyway+MySQL is the path to repeatable prod schema.

## 9. Docker Compose changes

```yaml
services:
  redis:
    image: redis:7-alpine
    container_name: fitcoach-redis
    ports: ["6379:6379"]
    volumes: [redis-data:/data]
    healthcheck:
      test: ["CMD","redis-cli","ping"]
      interval: 10s
      timeout: 3s
      retries: 5
    networks: [fitcoach]

  backend:
    depends_on:
      redis: { condition: service_healthy }
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
      JWT_SECRET: ${JWT_SECRET:?JWT_SECRET required}
      MIMO_API_KEY: ${MIMO_API_KEY:-}
      AI_COACH_PROVIDER: ${AI_COACH_PROVIDER:-mock}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:-http://localhost:5173}
    healthcheck:
      test: ["CMD","wget","-qO-","http://localhost:8080/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 10
      start_period: 40s

volumes:
  backend-uploads:
  redis-data:
```

## 10. Implementation order (handed to writing-plans)

1. Infra config: Redis, Actuator, Flyway profile-aware, static resource handler, multipart limit. Update `pom.xml` (`spring-boot-starter-data-redis`, `spring-boot-starter-actuator`, `flyway-core`, `flyway-mysql`, `tika-core`).
2. Extend `GlobalExceptionHandler`; add `spring.mvc.throw-exception-if-no-handler-found=true`.
3. Common DTO skeleton + bean-validation in `auth/` first (smallest, isolates Spring Validation wiring).
4. Build `infra/ai/AiCoachProvider` + `MockCoachProvider` + `MimoCoachProvider`. Wire `CoachService`; migrate `CoachController` off mocks.
5. `infra/sms`, `infra/mail`, `infra/storage` skeletons + mocks.
6. Auth completion: `VerifyCodeService`, phone login, forgot/reset, refresh blacklist (`RefreshTokenStore` writing both Redis and `t_refresh_token_blacklist`).
7. `RateLimitFilter` (Redis token bucket) + `LoginAttemptService` + 429 handler.
8. Pull service layers out of existing controllers: plan, social, leaderboard, admin, exercise. Controllers become thin.
9. `challenge/` package end-to-end + Flyway seed.
10. Static resource handler + upload validation (Tika + size + filename random).
11. `RequestLoggingFilter` + MDC; expose `X-Request-Id`.
12. Tests: repository → service → controller → end-to-end.
13. `application-prod.yml` hardening + headers + secret env validation.
14. Seed-data fix: real `itemsJson` for the 4 official plans, example challenges, ensure DataInitializer is idempotent.
15. Final pass: `OpenApiConfig` extraction, README update with new env vars and endpoints.

## 11. Risks / open assumptions

| Risk | Mitigation |
|------|-----------|
| MiMo `response_format=json_object` not yet supported on every model | Robust fallback: parse content as JSON; on failure wrap into `review` field |
| Embedded Redis library variance on Windows | Use Testcontainers in tests; degrade to skip Redis-dependent ITs if Docker unavailable |
| Flyway init script drifting from JPA entities | `ddl-auto: validate` in prod will fail-fast on mismatch |
| MaxUploadSizeExceededException can fire before Spring Security context exists | Handler registered as `@RestControllerAdvice` covers it |
| Refresh-token blacklist + Redis outage | Filter "fails open" for blacklist check but logs at WARN; auth still requires valid signature |
| `@CreatedDate` requires `@EntityListeners` — currently inconsistent | Audit pass during S5: every entity gets `@EntityListeners(AuditingEntityListener.class)` if it has audit fields |

## 12. Non-goals (explicit)

- Frontend code is not touched. The frontend `src/api/*.js` files dictate the contract.
- The pose-detection / scoring algorithms remain on the client.
- No premium / paid features are introduced.

## 13. S15 — Vector Memory details (added 2026-05-15)

### 13.1 Container change

```yaml
redis:
  image: redis/redis-stack-server:7.4.0-v0
  # remains password-less in dev; in prod set REDIS_ARGS or `--requirepass`
```

The OSS Redis Stack Server image bundles RediSearch 2.10+, which supports
`HNSW` and `FLAT` vector indexes with cosine / L2 / IP distance. No client
library change — Lettuce talks the same RESP protocol; vector commands are
just `FT.CREATE` / `FT.SEARCH` strings.

### 13.2 Index schema

Created on app boot via `VectorMemoryService.@PostConstruct`:

```
FT.CREATE idx:session:vec
  ON HASH PREFIX 1 sess:vec:
  SCHEMA
    user_id   TAG SORTABLE
    action    TAG
    score     NUMERIC SORTABLE
    created   NUMERIC SORTABLE
    text      TEXT NOSTEM
    embedding VECTOR HNSW 8
              TYPE FLOAT32
              DIM 1024
              DISTANCE_METRIC COSINE
              M 16
              EF_CONSTRUCTION 200
```

Document key: `sess:vec:<sessionId>`. Hash fields mirror the schema; `embedding` is the raw little-endian float32 byte string (Lettuce `set` with `byte[]` value).

### 13.3 EmbeddingProvider contract

```java
package com.fitcoach.infra.ai.embedding;

public interface EmbeddingProvider {
    /** Returns one float[1024] vector per input string. Implementations MUST be thread-safe. */
    List<float[]> embed(List<String> texts);
    int dimension(); // 1024 for current providers
    String name();   // "mock" | "mimo" | "openai"
}
```

Bean selection mirrors `AiCoachProvider`:
`@ConditionalOnProperty(name="ai.embedding.provider", havingValue="mimo")` →
`OpenAiCompatEmbeddingProvider`. Default `mock` so dev / tests run without
network.

`MockEmbeddingProvider`:
- Splits the text into UTF-8 bytes, SHA-256 hashes it
- Expands the 32-byte digest into a 1024-d float vector by `Random(seed=hashLong)` deterministic draw, then L2-normalized
- Identical text → identical vector → semantic-similarity tests are reproducible

`OpenAiCompatEmbeddingProvider`:
- POST `${ai.embedding.base-url}/embeddings`
- body `{"model":"<model>","input":[...]}`
- parses `data[].embedding`, returns float[]
- 10s timeout, exceptions wrap into `BusinessException(503, "Embedding provider unavailable")`

### 13.4 Configuration

```yaml
ai:
  embedding:
    enabled: true
    provider: ${AI_EMBEDDING_PROVIDER:mock}      # mock | mimo
    dimension: 1024
    mimo:
      api-key: ${AI_EMBEDDING_API_KEY:${MIMO_API_KEY:}}
      base-url: ${AI_EMBEDDING_BASE_URL:https://api.xiaomimimo.com/v1}
      model: ${AI_EMBEDDING_MODEL:mimo-embedding-v1}
      timeout-seconds: 10
  memory:
    enabled: true
    top-k: 5
    rebuild-batch-size: 200
```

If `embedding.enabled=false` OR `memory.enabled=false`, all RAG calls become no-ops and Coach falls back to S14 prompt context only.

### 13.5 Write path

`SessionCreatedEvent` (existing in S14) is consumed by **two** listeners:

1. `ProfileRefreshListener` (S14, profile aggregate)
2. `VectorMemoryListener` (S15, embed + hset + index update)

Both `@Async`, both `@TransactionalEventListener(phase=AFTER_COMMIT)`.
Order is irrelevant — they're independent. Failures in either are isolated and logged.

`VectorMemoryListener.handle(evt)`:

```java
String text = buildSessionText(session);            // "深蹲 30次 节奏=85 稳定=80 深度=70..."
float[] vec = embedding.embed(List.of(text)).get(0);
Map<String,Object> hash = Map.of(
    "user_id", String.valueOf(session.getUserId()),
    "action",  session.getAction(),
    "score",   String.valueOf(session.getScore()),
    "created", String.valueOf(session.getCreatedAt().toEpochSecond(UTC)),
    "text",    text,
    "embedding", floatsToLEBytes(vec)
);
redisTemplate.opsForHash().putAll("sess:vec:" + session.getId(), hash);
```

### 13.6 Read path (KNN)

`VectorMemoryService.searchSimilar(userId, queryVec, K)`:

```
FT.SEARCH idx:session:vec
  '(@user_id:{<userId>}) =>[KNN 5 @embedding $vec AS score]'
  PARAMS 2 vec <byte[]>
  RETURN 5 score text action created
  SORTBY score ASC
  DIALECT 2
```

Returns `List<Memory>` with `text/action/score/createdAt/distance`.

### 13.7 RAG injection in Coach prompt

`CoachPromptTemplates.userMessage` now appends a `recentMemories` JSON array (top-K, distance ≤ 0.45 to avoid junk) to the existing payload. System prompt gains a single line: `"参考用户最近的训练记录(recentMemories)调整建议,但不要逐条复述。"`

### 13.8 Rebuild endpoint

`POST /api/users/me/memory/rebuild` (auth required):

1. `SecurityUtil.currentUserId()` → uid
2. `sessionRepo.findByUserIdOrderByCreatedAtDesc(uid, PageRequest.of(0, 500))`
3. Embed in batches of `rebuild-batch-size` (default 200) — MiMo's `/embeddings` accepts arrays
4. Pipeline `HSET` writes
5. Return `{"reembedded": <count>, "took": <ms>}`

Admin-scope `POST /api/admin/memory/rebuild?userId=<id>` uses the same path with explicit uid; ROLE_ADMIN required.

### 13.9 Observability

- Counter `fitcoach.memory.embed.count{provider}` — incremented per embed call
- Histogram `fitcoach.memory.search.latency` — RediSearch FT.SEARCH duration
- Log line on every RAG injection: `[<requestId>] memory.search uid=<u> k=<k> hits=<n> took=<ms>ms`

### 13.10 Test plan

- `MockEmbeddingProviderTest` — same input twice → identical 1024-d vector, L2 norm ≈ 1
- `VectorMemoryServiceIT` — uses Testcontainers `redis/redis-stack-server`; insert 10 sessions for u=1, 10 for u=2, KNN search with u=1 only returns u=1 docs
- `CoachServiceTest` — when memory.enabled=true and Mock provider → ctx.recentMemories non-empty; when memory.enabled=false → empty
- `MemoryRebuildEndpointIT` — register, create 3 sessions, call rebuild, expect `reembedded=3`

### 13.11 Risks

| Risk | Mitigation |
|------|-----------|
| Redis Stack image larger (~120 MB) | Acceptable single-host deploy; documented |
| RediSearch FT.CREATE on idx that already exists throws "Index already exists" | Catch and ignore on startup; `FT.INFO` first |
| Embedding API rate / cost spikes from auto-rebuild loops | rebuild endpoint capped at 500 sessions per call; cooldown 60s per user |
| Float32 byte-order portability | Always little-endian (RediSearch convention); helper has unit test |
| Mock provider yields trivially clustered vectors | Mock is dev/test only; integration tests assert structure, not relevance |
