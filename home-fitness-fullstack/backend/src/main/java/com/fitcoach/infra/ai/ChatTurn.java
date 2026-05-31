package com.fitcoach.infra.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 陪伴聊天对话单轮 - role: user | assistant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatTurn {
    private String role;
    private String content;
}
