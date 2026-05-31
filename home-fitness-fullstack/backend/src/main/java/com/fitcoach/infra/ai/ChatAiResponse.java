package com.fitcoach.infra.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 陪伴聊天 provider 返回。reply 是自然语言（不强制 JSON），区别于 CoachAiResponse。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAiResponse {
    private String reply;
    private String provider;
    private int tokensUsed;
}
