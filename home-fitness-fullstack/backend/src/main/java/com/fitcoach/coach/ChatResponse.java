package com.fitcoach.coach;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 陪伴聊天对外响应。
 * reply 是自然语言回复；recalled 是这次调用真实"唤醒"的记忆片段（前端可显示"想起..."）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String reply;
    private String provider;
    private Integer tokensUsed;
    /** 此次回答用到的历史记忆片段（已剥离前缀），空数组表示没用上 */
    private List<String> recalled;
}
