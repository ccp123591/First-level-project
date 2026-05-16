package com.fitcoach.auth;

import com.fitcoach.exception.BusinessException;

/**
 * 密码强度策略 — 至少 8 位，包含至少 1 个字母和 1 个数字。
 */
public final class PasswordPolicy {

    private PasswordPolicy() {}

    public static void validate(String pw) {
        if (pw == null || pw.length() < 8) {
            throw new BusinessException(400, "密码至少 8 位");
        }
        boolean hasLetter = false, hasDigit = false;
        for (int i = 0; i < pw.length(); i++) {
            char c = pw.charAt(i);
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        if (!(hasLetter && hasDigit)) {
            throw new BusinessException(400, "密码必须同时包含字母与数字");
        }
    }
}
