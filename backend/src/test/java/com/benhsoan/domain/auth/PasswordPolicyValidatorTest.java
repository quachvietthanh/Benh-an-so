package com.benhsoan.domain.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.benhsoan.domain.auth.exception.WeakPasswordException;

class PasswordPolicyValidatorTest {

    @Test
    @DisplayName("valid password passes all checks without violations")
    void validPasswordPasses() {
        List<String> violations = PasswordPolicyValidator.validate("Admin@123");
        assertTrue(violations.isEmpty());
        assertDoesNotThrow(() -> PasswordPolicyValidator.validateOrThrow("Admin@123"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("blank password returns non-empty error")
    void blankPasswordFails(String blank) {
        List<String> violations = PasswordPolicyValidator.validate(blank);
        assertFalse(violations.isEmpty());
        assertThrows(WeakPasswordException.class, () -> PasswordPolicyValidator.validateOrThrow(blank));
    }

    @Test
    @DisplayName("short password fails length requirement (QTN-28 / TC-02)")
    void shortPasswordFails() {
        List<String> violations = PasswordPolicyValidator.validate("Aa1!");
        assertTrue(violations.stream().anyMatch(msg -> msg.contains("độ dài từ 8 đến 50")));
    }

    @Test
    @DisplayName("password without uppercase fails")
    void passwordWithoutUppercaseFails() {
        List<String> violations = PasswordPolicyValidator.validate("admin12345");
        assertTrue(violations.stream().anyMatch(msg -> msg.contains("in hoa")));
    }

    @Test
    @DisplayName("password without lowercase fails")
    void passwordWithoutLowercaseFails() {
        List<String> violations = PasswordPolicyValidator.validate("ADMIN12345");
        assertTrue(violations.stream().anyMatch(msg -> msg.contains("viết thường")));
    }

    @Test
    @DisplayName("password without digit fails")
    void passwordWithoutDigitFails() {
        List<String> violations = PasswordPolicyValidator.validate("AdminPassword");
        assertTrue(violations.stream().anyMatch(msg -> msg.contains("chữ số")));
    }

    @Test
    @DisplayName("weak password throws WeakPasswordException with all unmet criteria")
    void weakPasswordThrowsExceptionWithViolations() {
        WeakPasswordException exception = assertThrows(
                WeakPasswordException.class,
                () -> PasswordPolicyValidator.validateOrThrow("weak")
        );
        assertFalse(exception.getViolations().isEmpty());
    }
}
