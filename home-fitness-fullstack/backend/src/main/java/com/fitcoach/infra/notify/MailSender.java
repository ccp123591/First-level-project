package com.fitcoach.infra.notify;

/**
 * 邮件发送抽象 — 生产可换接 JavaMailSender / SES / SendGrid。Mock 实现仅打日志。
 */
public interface MailSender {
    /** 通用邮件发送（subject/text 由调用方拼装）。 */
    void send(String to, String subject, String text);

    /** 验证码邮件 — 模板化。 */
    void sendCode(String to, String code, String purpose);

    /** 实现名。 */
    String name();
}
