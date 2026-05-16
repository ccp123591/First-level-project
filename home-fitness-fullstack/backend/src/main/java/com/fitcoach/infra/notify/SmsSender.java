package com.fitcoach.infra.notify;

/**
 * 短信发送抽象 — 生产可换接真实运营商 SDK。Mock 实现仅打日志。
 */
public interface SmsSender {
    /**
     * @param phone   接收手机号（已粗校验）
     * @param code    明文验证码（mock 仅日志，真实实现走运营商 API）
     * @param purpose 业务用途 login / reset
     */
    void send(String phone, String code, String purpose);

    /** 实现名（用于日志 / 调试）。 */
    String name();
}
