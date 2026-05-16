package com.fitcoach.infra.notify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 默认短信发送实现 — 仅日志，不实际外呼。生产环境注入真实 SDK 实现并 @Primary 它覆盖此实现。
 */
@Slf4j
@Component
@Primary
public class LogSmsSender implements SmsSender {

    @Override
    public void send(String phone, String code, String purpose) {
        String masked = phone == null ? "" : phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
        log.info("[SMS-MOCK] to={} purpose={} code={}", masked, purpose, code);
    }

    @Override
    public String name() {
        return "log-sms";
    }
}
