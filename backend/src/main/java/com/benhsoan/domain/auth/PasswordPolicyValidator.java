package com.benhsoan.domain.auth;

import java.util.ArrayList;
import java.util.List;

import com.benhsoan.domain.auth.exception.WeakPasswordException;

/**
 * Domain service validating password strength policy (QTN-28 / NCL-01-CN-005 TC-02).
 */
public final class PasswordPolicyValidator {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 50;

    private PasswordPolicyValidator() {
    }

    public static List<String> validate(String password) {
        List<String> violations = new ArrayList<>();

        if (password == null || password.isBlank()) {
            violations.add("Mật khẩu không được để trống.");
            return violations;
        }

        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            violations.add("Mật khẩu phải có độ dài từ " + MIN_LENGTH + " đến " + MAX_LENGTH + " ký tự.");
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }

        if (!hasUpper) {
            violations.add("Mật khẩu phải chứa ít nhất một chữ cái in hoa (A-Z).");
        }

        if (!hasLower) {
            violations.add("Mật khẩu phải chứa ít nhất một chữ cái viết thường (a-z).");
        }

        if (!hasDigit) {
            violations.add("Mật khẩu phải chứa ít nhất một chữ số (0-9).");
        }

        return violations;
    }

    public static void validateOrThrow(String password) {
        List<String> violations = validate(password);
        if (!violations.isEmpty()) {
            throw new WeakPasswordException(violations);
        }
    }
}
